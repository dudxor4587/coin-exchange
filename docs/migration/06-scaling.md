# 6단계 — 부하 분해와 확장 (작성 중)

> 이 챕터는 진행 중이다. 아래는 ① 병목 측정까지의 raw 기록이고,
> 커넥션 풀 튜닝 / HPA 측정이 끝나면 서사로 다시 정리한다.

## 왜 측정부터인가
원래 6단계 계획은 "k8s + HPA 도입"이었다. <br>
그런데 Kafka 측정에서 배운 원칙 — *기술 도입엔 데이터 근거가 있어야 한다* — 을 그대로 적용하면,
HPA를 붙이기 전에 *무엇이 병목인지부터 측정* 해야 한다. <br>
병목을 모르고 HPA를 붙이면 *엉뚱한 것을 복제* 하게 된다. <br>

특히 매칭엔진을 두고 *"매칭이 가장 부하가 심하니 거기를 확장해야 한다"* 고 막연히 생각하기 쉬운데,
이게 사실인지부터 데이터로 갈라야 했다. <br>

## ① 병목 측정 — 주문 흐름 구간 분해
`OrderFlowService`의 각 구간에 Micrometer Timer를 붙여(`order.flow.segment`), 어디서 시간이 가는지 쟀다. <br>
부하: K6 200 VU, 1분 20초, 매수/매도 혼합 (docker-compose, RabbitMQ). <br>

구간별 평균(sum/count):

| 구간 | 평균 | 비중 | 정체 |
|---|---|---|---|
| createOrder (DB insert) | 2,413 ms | 76% | DB 커넥션 풀 대기 |
| debitCoin (HTTP → funds, 매도) | 722 ms | ~22% | 동기 RPC |
| debitKrw (HTTP → funds, 매수) | 605 ms | ~19% | 동기 RPC |
| processMatches (settle 등, 매칭 시) | 140 ms | 4% | |
| placeOrderBook (Redis) | 4.4 ms | 0.1% | |
| **match (매칭 연산, Redis Lua)** | **4.3 ms** | **0.1%** | 거의 공짜 |

매수 1건 ≈ debitKrw(605) + createOrder(2413) + match(4) + placeOrderBook(4) + processMatches(140) ≈ 3,170 ms. <br>
전체 처리량 50/s, p95 약 8초. <br>

## 측정이 말해준 것
**매칭 연산 자체는 4.3ms — 전체의 0.1%다.** <br>
*"매칭이 병목"이라는 통념이 데이터로 반증됐다.* 매칭(Redis Lua)은 거의 공짜고,
진짜 병목은 *그 주변* — DB 쓰기(createOrder)와 동기 RPC(debit)다. <br>

특히 createOrder가 2.4초로 76%를 차지하는데, INSERT 한 줄이 2.4초일 리는 없다. <br>
이건 *HikariCP 커넥션 풀 고갈* 이다 — 200 VU가 동시에 createOrder에서 DB 커넥션을 요구하는데
기본 풀 크기는 10개라, 나머지가 커넥션을 기다리는 시간이 2.4초로 누적된 것이다. <br>

이 발견은 이전 챕터(매칭엔진 DB vs Redis 비교)의 결론 — *진짜 병목은 DB 풀 공유* — 와
정확히 연결된다. 도메인을 분리하고 DB 인스턴스도 나눴지만, *한 서비스 안에서의 풀 경합* 은 여전히 남아 있었다. <br>

## 확장 전략에 주는 함의
구간별로 병목이 다르니 확장 전략도 갈린다. <br>
- match (4ms) → 확장 불필요. HPA도 샤딩도 필요 없는 수준.
- createOrder (2.4s, DB 풀 경합) → 커넥션 풀 튜닝, 그래도 부족하면 pod 복제로 풀 총량 증가.
- debit/settle (HTTP RPC) → funds pod 복제로 처리 용량 증가.

즉 *HPA로 복제해야 할 것은 매칭이 아니라 DB·HTTP 처리 용량* 이고,
그 전에 *커넥션 풀 튜닝* 이라는 더 싼 수단을 먼저 시도해야 한다. <br>

## 다음 (진행 예정)
- ② 커넥션 풀 튜닝 — HikariCP 10 → N, createOrder 시간/처리량 재측정
- ③ HPA — kind 재구축 + Prometheus + Custom Metrics Adapter, pod 자동 확장 before/after
- ④ 한계 인식 — 매칭은 코인당 직렬화 지점이라 복제 불가, 진짜 확장은 심볼 샤딩 (별도 과제)
- ADR — k8s vs Docker Swarm
