MSA로 나누고 운영 이슈(회로차단기·트레이싱·헬스체크)까지 손봤으니 이제 이 분산 시스템을 부하로 밀어 병목이 어디인지를 실제로 잰다. <br>
[in-process 매칭 측정](../매칭엔진%20in-process%20측정%20—%20왕복%20제거.md)에서 매칭은 이제 병목이 아니고 남는 건 그 주변, 돈이 걸린 동기 RPC(debit/settle)와 로그 append라고 봤다. 그 예측이 분산 환경에서 맞는지 확인한다. <br>

# 어떻게 쟀나
주문 hot path에는 구간별 타이머(`order.flow.segment`)가 이미 심겨 있다. debitKrw → nextId → appendLog → placeOrderBook → match → processMatches 순이다. <br>
k6로 게이트웨이에 주문 부하를 주고, trading의 그 타이머를 부하 창(window) 전후로 스냅샷 떠서 차분했다. 액추에이터 타이머는 앱 시작 이후 누적이라 before/after를 빼야 창 안의 값만 남는다. <br>
매번 Redis 오더북을 FLUSHALL해 누적 상태를 지우고 시작했다(예전에 누적 상태 오염으로 측정을 망친 적이 있다). <br>

# 병목은 funds(돈 경로)였다
40 VU로 45초, 100% 성공한 런의 구간별 평균이다. <br>

| segment | mean ms | 정체 |
|---|---|---|
| debitCoin (매도 첫 단계) | 146 | funds RPC |
| debitKrw (매수 첫 단계) | 103 | funds RPC |
| processMatches (settle + kafka) | 51 | funds RPC |
| match | 15 | Redis 왕복 |
| placeOrderBook | 11 | Redis 왕복 |
| nextId | 11 | Redis 왕복 |
| appendLog | 1 | Kafka |

주문 하나(매수) ~192ms 중 돈 경로(debit 103 + settle 51)가 ~80%, Redis 3왕복(37ms)이 ~20%, kafka는 1ms로 무시할 수준이다. <br>
예측대로 매칭·저장소는 부차적이고 **funds RPC가 지배적**이다. <br>

# 지갑 락인가, funds 자체인가
debit이 100ms를 넘는 게 이상했다. k6가 유저를 2명만 쓰는데, 지갑 차감이 비관락(`@Lock(PESSIMISTIC_WRITE)`)이라 **모든 매수가 유저1 지갑 한 행에 직렬화**된다. 이게 debit을 인위적으로 부풀렸을 수 있다. <br>
그래서 유저 40명과 지갑을 시딩해 VU마다 다른 유저를 쓰게 하고 다시 쟀다. 지갑을 2개에서 40개로 흩었는데 debit은 103→87ms(매수), 146→128ms(매도)로 **12~16%만** 내렸다. <br>
즉 지갑 락 경합은 곁가지였고, funds 비용의 대부분은 **동기 RPC + DB 트랜잭션 + fsync 왕복 그 자체**다. Redis 한 왕복(~10~15ms)의 열 배쯤 되는 이 비용은 funds 구조에서 온다. 튜닝으로 줄일 수 있는 게 아니다. <br>

# 어디서 깨지나
부하를 200 VU로 올리니 84%가 거절됐다(성공 16%). 거절은 전부 debit 단계에서 났다. debit 카운트는 3.5만인데 그 다음 단계(nextId)는 6천뿐이었다. 나머지 2.9만 주문이 funds RPC에서 실패해 뒷단계로 가지 못한 것이다. <br>
funds가 ~130 req/s 부근에서 한계에 걸리고, 그 위로는 debit이 타임아웃·서킷으로 떨어진다. <br>

# 정리
- 분산 환경의 병목은 매칭이 아니라 **funds(돈 경로 RPC)**다. 세 번의 부하(40 VU 2유저 / 40 VU 다유저 / 200 VU)가 같은 곳을 가리켰다. <br>
- 스케일해야 할 건 funds다. CPU 기준 HPA로 trading을 늘려도 funds가 막혀 소용없다. 계획서가 6단계에서 예측한 그대로다. <br>
- 지갑 비관락은 부차적(~15%)이고, 실제로 손대야 할 건 돈 경로의 동기성이다. settle을 비동기로 빼거나 funds와 그 DB를 스케일하는 방향이 다음 후보다. <br>
- HPA 자체의 스케일링 동작은 아직 안 쟀다. HPA 매니페스트가 없다. 이 측정이 정한 건 "무엇을 스케일할지(funds)"까지다. <br>
- 절대 수치는 로컬 도커라 부풀어 있다(Redis 한 왕복이 µs가 아니라 10~15ms로 잡힌다). 상대 비교와 병목 위치가 요점이다. <br>
