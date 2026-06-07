1~4단계를 거치면서 시스템은 *코드도 프로세스도 데이터도 진입점도* 전부 분리된 상태가 됐다. <br>
다음으로 자연스럽게 잡았던 단계는 메시징 백본을 RabbitMQ에서 Kafka로 옮기는 것이었다 — 6단계의 수평 확장을 미리 받쳐주기 위해서다. <br>

처음 잡은 계획은 단순했다.
1. RabbitMQ 17개 큐를 Kafka 토픽으로
2. 파티션 키 = coinId 로 코인별 순서 보장 + 병렬 처리
3. 컨슈머 그룹으로 인스턴스 확장 시 자동 재분배

그런데 작업 단위를 짜다가 *방향을 한 번 더 바꾸게 됐다*. <br>

# 깨달음 — "이벤트 만능"에서 벗어나는 시점
이전 챕터들에서 나는 *모든 도메인 통신은 이벤트가 옳다* 는 입장에 가까웠다. <br>
EDA 챕터에서 그렇게 결정했고, 그 위에 SAGA 패턴, RabbitMQ, 1~4단계 분리가 모두 그 가정 위에 쌓여 있었다. <br>

5단계 계획을 짜는 도중에 그 가정 자체에 의문이 들기 시작했다. <br>
매수 흐름을 다시 그려보면 다음과 같다. <br>
```
사용자 매수 요청
  → OrderCreated (이벤트) → wallet 잔고 잠금
  → OrderReady (이벤트) → OrderBook 등록
  → (background @Scheduled) 매칭 → TradeCreated (이벤트)
  → OrderMatched (이벤트) → 잔고 정산 (이벤트 더)
  → 사용자에게 *언젠가* 결과
```

사용자는 즉시 응답을 받아야 하는데 여러 단계의 비동기 체인으로 풀고 있었다. <br>
그리고 이 모든 흐름이 *우리 시스템 안* 에서 일어나는 일이다. 외부 시스템과의 경계가 아니다. <br>
한 트랜잭션으로 끝낼 수 있는 일을 굳이 분산 패턴으로 풀고 있었던 셈이다. <br>

Outbox 패턴, 컨슈머 멱등성, SAGA 보상까지 다 갖춰도 결국 결국 정합성이 *언젠가* 맞춰지는 시스템이다. <br>
사용자 입장에서 "내 잔고 차감됐나"의 답을 언젠가 주는 시스템. <br>
하지만 이건 *우리가 통제할 수 있는 일* 이고, *동기로 풀면 즉시 답이 나오는 일* 이었다. <br>

# 동기가 맞는 경계 / 이벤트가 맞는 경계
깨달음 직후 정리한 기준은 다음과 같다. <br>

| 동기가 맞는 경우 | 이벤트가 맞는 경우 |
|---|---|
| 사용자가 즉시 결과를 알아야 함 | 부수적 작업 (감사/알림/통계) |
| 정합성 깨지면 돈/법적 문제 | 일시적 불일치가 무해 |
| 한 트랜잭션 안에서 끝남 | 외부 시스템 경계 (필연적 분산) |
| 호출 빈도가 낮음 | 호출 빈도가 매우 높음 |

이 기준으로 우리 도메인을 다시 분류해 보면 다음과 같다.
- **매수/매도 잔고 잠금 + 매칭 + 정산** → 왼쪽. 사용자 즉시 응답 + 우리 시스템 안 + 정합성 핵심.
- **입금/출금 승인** → 왼쪽. 어드민이 승인하는 즉시 잔고에 반영되어야 함.
- **알림** → 오른쪽. 실패해도 사용자 경험에 치명적이지 않음.
- **체결 기록 (감사용)** → 오른쪽. 다운스트림 소비처(시세 피드, 회계).
- **롤백/실패 보상** → 오른쪽. 그 자체가 비동기 보상 흐름.

지금까지 *전부 오른쪽으로* 풀고 있었다는 게 보였다. <br>
5단계의 진짜 작업이 *Kafka로 갈아끼우기* 가 아니라 **적재적소 재조정**이라는 게 명확해졌다. <br>

# 지금 손보는 이유
깨달음을 회고에만 적고 구조는 그대로 두는 선택지도 있었다. <br>
하지만 마이그레이션 비용을 따져보니 *지금 손대는 게 훨씬 싸다* 는 결론이 나왔다. <br>

만약 5단계를 원래 계획대로 끝낸다고 가정하면 다음 상태가 됐을 것이다.
1. Outbox 테이블이 4개 publisher 서비스에 박혀 있고
2. Kafka 토픽 17개에 매일 메시지가 흐르고
3. 컨슈머마다 dedup 테이블이 누적되고
4. 5단계 회고 문서가 *"분산 정확성을 갖춘 이벤트 기반 시스템"* 으로 쓰여 있고

이 상태에서 *"사실 동기가 맞았다"* 로 갈아엎는 비용은 5단계를 통째로 다시 만드는 것에 가깝다. <br>
지금 단계에서 손대면 1주차 작업의 일부 *방향만 바꾸는* 것으로 끝난다. <br>

# 한 일 1 — funds-service에 내부 RPC
먼저 잔고 조작을 동기 호출로 가능하게 하는 내부 API를 funds-service에 만들었다. <br>
```
POST /internal/funds/krw/credit   { userId, amount }   ← 입금/매도 정산
POST /internal/funds/krw/debit    { userId, amount }   ← 매수 잠금/출금
POST /internal/funds/coin/credit  { userId, coinId, amount }   ← 매수 체결
POST /internal/funds/coin/debit   { userId, coinId, amount }   ← 매도 잠금
POST /internal/funds/settle       { buyerId, sellerId, coinId, matchedAmount, totalKrw }
```

`/settle`이 흥미로운 결정이었다. <br>
매칭이 체결되면 *buyer 코인 증가 + seller KRW 증가* 가 한꺼번에 일어나야 하는데, 두 번의 동기 호출로 풀면 부분 성공이 가능해진다. <br>
하지만 둘 다 같은 funds-db에 있으니, funds-service 안에서 한 `@Transactional` 메서드로 묶으면 원자성이 보장된다. <br>
이런 원자성 묶음은 SAGA로 풀 일이 아니라 *서비스 내부 한 트랜잭션* 으로 풀어야 한다는 점이 다시 확인됐다. <br>

경로 접두어는 4단계에서 정한 *외부/내부 분리* 컨벤션을 그대로 따랐다. <br>
`/internal/**`은 gateway 라우팅에서 제외되어 docker 내부망에서만 접근할 수 있다 — 외부 노출 위험이 없다. <br>

# 한 일 2 — trading-service의 동기 흐름
매수/매도의 흐름을 *이벤트 체인* 에서 *한 메서드 안의 동기 시퀀스* 로 옮겼다. <br>

새 모델:
```
OrderController (@CurrentUserId)
  ↓
OrderFlowService.placeBuyOrder
  ├─ fundsClient.debitKrw       ← 동기 RPC to funds-service
  ├─ orderService.createBuyOrder (Order 저장)
  ├─ orderBookService.placeOrder (Redis)
  ├─ matchingEngine.match()     ← Redis Lua, sub-ms
  └─ for each match:
       ├─ tradeService.createTrade
       ├─ fundsClient.settle    ← 동기 RPC, 양쪽 정산 원자적
       ├─ orderService.fillOrder × 2
       └─ NotificationRequested 이벤트 (← 여기는 비동기 유지)
```

가장 큰 구조적 변화는 `MatchingEngineServiceWithRedis`의 `@Scheduled(500ms)`를 제거한 것이다. <br>
이전 모델은 *주문이 들어왔는지와 무관하게* 0.5초마다 매칭을 시도했는데, 사실 매칭이 일어날 수 있는 시점은 *새 주문이 들어왔을 때* 뿐이다. <br>
새 주문 없이 OrderBook이 가만히 있으면 매칭될 일도 없다. <br>
그래서 *주문 진입 시점에 직접 트리거* 하는 것으로 충분하다 — 동시에 훨씬 단순한 모델이 된다. <br>

`OrderFlowService`는 trading-service의 최상위 조립 지점에 둔다. <br>
도메인 모듈(`domain-order`, `domain-trade`)은 자기 엔티티 CRUD만 책임지고, *여러 도메인을 가로지르는 흐름 조립* 은 최상위에 두는 계층 구조는 이전 챕터들의 패턴과 일치한다. <br>

# 한 일 3 — 사라진 코드
동기 전환과 함께 *대거 dead code* 가 됐다. 약 800줄 삭제. <br>

**trading-service에서 사라진 것**
1. `BuyOrderReadyEventListener`, `SellOrderReadyEventListener` — 동기로 풀어서 불필요.
2. `OrderMatchedEventListener`, `OrderProcessingFailedEventListener` — 동기 응답으로 처리.
3. `BuyOrder*EventHandler`, `SellOrder*EventHandler`, `OrderMatched/TradeCreatedEventHandler` 8개 — Spring 이벤트 → RabbitMQ 다리 역할이 더는 필요 없음.
4. `Order` 엔티티의 `@DomainEvents` + `BuyOrderFilledEvent` 등 자동 발행 로직 — fill()의 부수 효과 제거.
5. `MatchingEngineServiceWithRedis`의 `@Scheduled` + `TradeCreatedEvent` 발행.

**funds-service에서 사라진 것**
1. `BuyOrderCreatedEventListener`, `SellOrderCreatedEventListener`, `BuyOrderFilledEventListener`, `SellOrderFilledEventListener`, `BuyOrderCompletedEventListener`, `SellOrderCompletedEventListener` — 6개 listener.
2. `BuyOrderReadyEventHandler`, `SellOrderReadyEventHandler`, `OrderProcessingFailedEventHandler` — 3개 publisher.
3. `WalletService.processBuyOrder`, `processSellOrderFill`, `CoinWalletService.processSellOrder`, `processBuyOrderCompletion` 등 — 동기 API와 중복되는 메서드들.

복잡도가 눈에 보이게 줄었다. <br>
*원래 단순했던 흐름을 분산 패턴으로 복잡하게 풀고 있었음* 이 코드 줄 수로도 드러난다. <br>

# 부산물 — 인증 정리
5단계 작업 중 인증 관련도 한 번 더 정리했다 (4단계의 마무리에 가깝다). <br>

1. 4단계에서 `app/` 모듈을 삭제하면서 `WebConfig`도 같이 사라져 있었다. `@CurrentUserId` resolver가 등록 안 된 상태였는데, 다행히 K6는 `/api/orders/*`에서 하드코딩 userId를 썼기 때문에 안 깨졌었다.
2. `CurrentUserIdArgumentResolver`를 cookie + JwtTokenProvider 기반에서 *SecurityContext 기반* 으로 바꿨다. 4단계에서 gateway가 토큰 검증 후 X-User-Id 헤더를 박고, 뒷단의 `HeaderAuthenticationFilter`가 SecurityContext에 그걸 박아두는 흐름이 이미 있어서, resolver는 그걸 읽기만 하면 된다.
3. 새 `WebMvcSecurityConfig`를 common-core에 두어 모든 서비스가 공유하도록 했다.

# 한 일 4 — 입금/출금 승인도 동기로
매수/매도와 같은 패턴으로, deposit-service와 withdraw-service의 *승인* 흐름도 동기 RPC로 바꿨다. <br>

승인은 어드민이 클릭하는 즉시 잔고에 반영되어야 하는 *정합성 도메인* 이다. <br>
사용자 입장에서 "내 입금 처리됐나"의 답을 즉시 받아야 한다. <br>
이전엔 `DepositApprovedEvent` → 큐 → 컨슈머 → 잔고 증가의 비동기 체인이었는데, 동기로 직접 호출하도록 바꿨다. <br>

```
DepositAdminController
  ↓ (admin 권한 체크)
DepositApprovalService.approve  ← @Transactional
  ├─ deposit.approve() (Deposit 도메인)
  ├─ walletService.creditKrw (직접 호출 — 같은 funds-service 안)
  └─ NotificationRequestedEvent (비동기)
```

`WalletService`를 같은 서비스(funds-service) 내부에서 직접 부르는 패턴이다 — RPC 거치지 않고 같은 프로세스 안에서. <br>
`OrderFlowService`가 `FundsClient`로 *교차 서비스* 호출인 것과 대비된다. <br>
*분리는 서비스 경계에서만 하고, 내부에선 직접 호출* 하는 게 합리적이라는 원칙이 다시 확인된다. <br>

`DepositApprovedEventHandler`, `DepositApprovedEventListener`, `WithdrawApprovedEventHandler`, `WithdrawApprovedEventListener`, `WithdrawFailedEventListener` 모두 dead code가 되어 삭제했다. <br>
`WithdrawFailedEvent`도 더 이상 발행되지 않는다 — 동기 흐름에서 잔고 차감 실패는 트랜잭션 롤백으로 끝나기 때문이다. <br>

거절 흐름(`DepositRejectedEvent`, `WithdrawRejectedEvent`)은 *알림 트리거* 라 비동기로 유지했다. <br>

# 한 일 5 — 비동기로 남은 것, 그리고 메시징 백본 재검토
동기 전환이 끝나고 나니 *진짜 비동기로 남은 이벤트* 는 알림 계열뿐이었다. <br>
- `NotificationRequestedEvent` (체결/입출금 알림)
- `DepositRejectedEvent`, `WithdrawRejectedEvent` (거절 알림)

이 남은 것들을 *Kafka로 옮기는 게 이 챕터의 원래 목표* 였다. <br>
Kafka를 택했던 이유는 두 가지였다. <br>
1. **순서 보장** — 파티션 키를 userId로 잡으면 같은 사용자의 알림이 발행 순서대로 처리된다.
2. **수평 확장** — 6단계에서 컨슈머를 늘릴 때 컨슈머 그룹이 파티션을 자동 재분배한다.

그래서 실제로 *Kafka로 마이그레이션했고*, 그 위에 발행 보장을 위한 **Outbox 패턴** 과 중복 처리를 막는 **컨슈머 멱등성(eventId + dedup 테이블)** 까지 얹었다. <br>
5단계를 *그 상태로 끝낼 수도 있었다*. <br>

그런데 6단계로 넘어가기 직전, *"기술 도입엔 데이터 근거가 있어야 한다"* 는 생각에 이 결정을 다시 들여다봤다. <br>
Kafka를 택한 첫 번째 이유가 *순서 보장* 인데, **RabbitMQ가 정말 순서를 못 지키는지, Kafka는 지키는지를 측정한 적이 없었다**. <br>
근거 없이 도입한 셈이라, 측정부터 하기로 했다. <br>

# 측정 — RabbitMQ는 정말 순서를 못 지키는가
같은 측정 도구를 양쪽 백본에 대칭으로 붙였다. <br>
같은 userId로 `ORDSEQ:1` .. `ORDSEQ:2000` 을 *완벽한 오름차순* 으로 일제히 발행하고, 컨슈머가 처리한 순서에서 *seq가 뒤집힌 횟수(inversions)* 를 셌다. <br>
컨슈머가 실제 알림 발송처럼 *가변 지연(3ms)* 을 갖도록 두고, 컨슈머 스레드 수(concurrency)를 1/2/4로 바꿔가며 쟀다. <br>

결과(count=2000): <br>

| concurrency | RabbitMQ inversions | Kafka inversions |
|---|---|---|
| 1 | 0 | 0 |
| 2 | 742 (37%) | 0 |
| 4 | 878 ~ 902 (≈44%) | 0 |

RabbitMQ는 컨슈머를 늘리는 순간 순서가 무너졌다. <br>
단일 큐는 *공유 작업 풀* 이라, 컨슈머 N개가 메시지를 각자 집어 병렬 처리하면 *완료 순서가 발행 순서와 어긋난다*. 처리량을 얻으려고 컨슈머를 늘리면 순서를 잃는 trade-off다. <br>

Kafka는 concurrency를 4로 올려도 inversions가 0이었다. <br>
파티션 분포를 직접 확인해 보니, key=userId=1로 발행한 메시지가 *전부 한 파티션(partition 7)으로* 갔다. <br>
같은 키는 고정 파티션, 한 파티션은 한 컨슈머 스레드 — 그래서 concurrency와 무관하게 순서가 유지된다. 동시에 다른 키는 다른 파티션이라 병렬 처리된다. <br>
*"순서 + 처리량 동시 확보"* 가 Kafka가 RabbitMQ보다 나은 지점이고, 측정으로 그게 확인됐다. <br>

여기까지만 보면 Kafka가 명백히 옳다. <br>
그런데 측정은 *"Kafka가 순서를 지킨다"* 를 증명했을 뿐이다. *"그 순서가 우리에게 필요한가"* 는 다른 질문이었다. <br>

# 재해석 — 그 순서가 우리에게 필요한가
처음 Kafka를 계획할 때 파티션 키를 *coinId* 로 잡으려 했었다. <br>
*코인별 매수/매도 주문 순서* 가 보장돼야 한다고 생각했기 때문이다. <br>
거래소에서 주문 순서는 생명이고, 그걸 메시징 백본이 지켜줘야 한다고 봤다. <br>

그런데 이 챕터에서 *매수/매도 흐름을 전부 동기로 되돌렸다* (한 일 1~3). <br>
이제 주문 순서는 *메시지 큐가 아니라 Redis 매칭엔진의 단일 직렬화 지점* 에서 잡힌다. <br>
주문이 들어오면 동기로 OrderBook에 등록되고 Lua 스크립트 안에서 매칭이 순서대로 일어난다. <br>
**코인별 주문 순서는 더 이상 메시징 백본의 책임이 아니다.** <br>

그러면 메시징으로 순서가 중요한 게 뭐가 남나. <br>
비동기로 남은 건 *알림뿐* 이다. <br>
그리고 알림 순서는 그리 중요하지 않다. <br>
- 알림은 타임스탬프를 달고 가니 UI에서 정렬할 수 있다.
- "매수 체결" 알림과 "매도 체결" 알림이 몇 밀리초 뒤바뀌어도 사용자 경험에 치명적이지 않다.

즉 측정으로 확인된 *Kafka의 순서 우위(44% vs 0%)* 가, *우리 시스템에서는 실질적 가치를 갖지 못한다*. <br>

# 결정 — RabbitMQ로 되돌린다
순서 우위가 우리 맥락에서 가치가 없다면, Kafka를 유지할 이유가 약하다. <br>
그래서 **Kafka 마이그레이션을 되돌리고 RabbitMQ로 돌아갔다**. <br>
Kafka 위에 얹었던 *Outbox 패턴* 과 *컨슈머 멱등성* 도 함께 되돌렸다. <br>

셋을 같이 되돌린 건 우연이 아니다. <br>
Kafka(순서 보장), Outbox(발행 보장), dedup(중복 처리 방지) — 셋 다 *강한 분산 보장 장치* 인데, 현재 비동기로 남은 건 알림뿐이고 *알림에는 이 보장들이 모두 과하다*. <br>
- Kafka의 순서 보장 → 알림 순서는 critical하지 않음
- Outbox의 발행 보장 → 알림은 유실돼도 치명적이지 않음 (RabbitMQ의 AFTER_COMMIT 발행으로 충분)
- dedup 멱등성 → 알림 한 번 더 가는 건 문제가 아님

그럼 거래소들은 왜 Kafka를 쓰나. <br>
조사해 보니 거래소가 Kafka를 쓰는 진짜 이유는 *순서* 가 아니라 **체결 스트림의 fan-out** 이었다. <br>
체결 하나를 *시세 피드, 정산, 리스크 관리, 감사 로그, 알림* 이 동시에 소비하고, 새 분석 시스템이 과거 체결을 재처리하는 구조 — 이건 *소비하면 사라지는* RabbitMQ로는 불가능하고 Kafka의 로그 보존이 필수다. <br>
우리는 현재 그 fan-out이 *알림 하나뿐* 이라 Kafka의 진짜 가치를 절반도 못 쓴다. <br>

그래서 결론은 **YAGNI** 다 — 필요해지기 전엔 도입하지 않는다. <br>
지금 규모/기능엔 RabbitMQ로 충분하고, *체결 스트림을 여러 시스템이 소비하는 fan-out 요구가 생기는 시점* 에 Kafka(와 Outbox, dedup)를 함께 재도입하면 된다. 그건 별도의 "거래소다운 기능" 챕터에서 마주할 것이다. <br>

한 가지 예외는 *OrderBook race 수정* 이다. <br>
이건 Outbox를 도입하며 발견했지만 *메시징 백본과 무관한 버그* 라, 되돌리지 않고 유지했다 (`placeBuyOrder`의 `@Transactional` 분리 + `MatchProcessor` 추출). 자세한 내용은 아래. <br>

## 유지한 것 — OrderBook race 수정
`OrderFlowService.placeBuyOrder`에 `@Transactional`이 걸려 있으면, *Order 저장 → OrderBook(Redis) 등록 → 매칭* 이 한 트랜잭션 안에 묶인다. <br>
Order는 트랜잭션 끝에 commit되는데 *OrderBook(Redis)은 즉시 등록* 된다. 두 자원이 트랜잭션으로 묶이지 않는다. <br>
다른 스레드의 매칭 시점에 OrderBook에는 보이는데 DB에는 아직 안 보이는 간극이 생기고, 그 사이 `fillOrder`가 *commit 전 row* 를 못 찾아 `ORDER_NOT_FOUND`가 터진다. <br>

수정은 다음과 같다.
1. `placeBuyOrder`/`placeSellOrder`의 `@Transactional` 제거.
2. `orderService.createBuyOrder`만 자체 `@Transactional`로 Order를 먼저 commit → OrderBook 등록은 commit 후.
3. `processMatch`는 별도 빈(`MatchProcessor`)으로 추출 — 매칭마다 독립 트랜잭션 + 자기 자신 호출 회피.

```java
public void placeBuyOrder(...) {
    fundsClient.debitKrw(...);
    Order order = orderService.createBuyOrder(...);  // 자체 @Transactional로 commit
    orderBookService.placeOrder(order);              // commit 후에 OrderBook
    for (match : matchingEngine.match()) {
        matchProcessor.processMatch(match);          // 각 매칭이 별도 @Transactional
    }
}
```

이 race는 *어떤 메시징 백본을 쓰든 잠재해 있던 결함* 이다. 부하가 낮을 땐 우연히 안 터졌을 뿐이고, 트랜잭션 path가 길어지면 드러난다. 그래서 백본을 되돌려도 이 수정만은 남겨두는 게 맞다. <br>

# 검증
되돌리기 전, Kafka + Outbox + 멱등성을 다 갖춘 상태의 K6 측정값을 먼저 남겨둔다 (그 비용이 결정의 근거였으니까). <br>

| 단계 | 처리량 | p95 | 에러 |
|---|---|---|---|
| 4단계 (gateway) | 449/s | 494ms | 0% |
| 5단계 중간 (sync RPC + Outbox + Kafka) | 100/s | 2.5s | 0% |
| 5단계 (+ 컨슈머 멱등성) | 62/s | 4.8s | 0% |

4단계 대비 응답 시간이 약 10배, 처리량이 약 7배 떨어졌다. <br>
추가된 비용을 분해하면 다음과 같다.
1. 동기 RPC — 매수 흐름에 `funds.debitKrw` + `funds.settle` 두 번의 HTTP hop. *이건 정합성을 위한 비용이라 유지된다.*
2. Outbox INSERT + 폴링 — 모든 발행에 DB INSERT, 릴레이의 SELECT FOR UPDATE. *되돌리며 사라짐.*
3. dedup 체크 — 컨슈머마다 SELECT + INSERT. *되돌리며 사라짐.*

처리량 100 → 62/s 하락은 거의 전부 *컨슈머 멱등성* 에서 왔는데, 그 멱등성이 지키던 게 *알림 중복 방지* 였다. <br>
*알림이 어쩌다 한 번 더 가는 걸 막으려고 처리량의 38%를 지불* 하고 있던 셈이고, 이 수치가 되돌림 결정을 뒷받침했다. <br>

# 결론
> 이 챕터는 두 개의 판단으로 이뤄져 있다. <br>
> 첫째, **"이벤트 만능에서 적재적소로"** — 정합성이 중요한 매수/매도/입출금 승인을 *동기로 되돌렸다*. 이건 유지된다. <br>
> 둘째, **"근거 없이 도입한 Kafka를 측정하고 되돌렸다"** — 순서 보장이라는 명분으로 Kafka를 도입했지만, 측정해 보니 그 순서가 우리 시스템(비동기는 알림뿐)에는 실질적 가치가 없었다. RabbitMQ로 돌아가고, Kafka·Outbox·dedup을 *fan-out 요구가 생기는 시점* 으로 미뤘다. <br>
> 5단계의 나는 *Kafka가 옳다* 고 믿고 도입했고, 6단계 직전의 나는 *측정으로 그 믿음을 검증* 해 되돌렸다. 기술을 "있어 보여서"가 아니라 "필요해서, 데이터로" 쓰는 것 — 이 챕터가 남기는 건 그 판단의 과정이다.
