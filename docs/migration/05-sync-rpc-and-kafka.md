# 5단계 — 적재적소로 다시 잡기 (sync RPC + Kafka)

## 배경 — 5단계는 원래 Kafka 마이그레이션이었다

여기까지 1~4단계를 오면서 시스템은 *코드도, 프로세스도, 데이터도, 진입점도* 전부 분리된 상태가 됐다.
다음 자연스러운 단계는 메시징 백본을 RabbitMQ에서 Kafka로 옮기는 것이었다 — 6단계의 수평 확장을 wire 레벨에서 미리 지원하기 위해서.

처음엔 그렇게 계획을 잡았다.
- RabbitMQ 17개 큐를 Kafka 토픽으로
- 파티션 키 = coinId 로 코인별 순서 보장 + 병렬 처리
- 컨슈머 그룹으로 인스턴스 확장 시 자동 재분배

그런데 작업 단위를 짜다가 *방향을 한 번 더 바꾸게 됐다*.

## 깨달음 — "이벤트 만능"에서 벗어나는 시점

이전 챕터에서 나는 *모든 도메인 통신은 이벤트가 옳다*라는 입장에 가까웠다. EDA 챕터에서 그렇게 결정했고, 그 위에 SAGA 패턴, RabbitMQ, 1~4단계 분리가 다 그 가정 위에 쌓여 있었다.

5단계 계획을 짜는 도중에 그 가정 자체에 의문이 들기 시작했다.

매수 흐름을 다시 그려 보면:
```
사용자 매수 요청
  → OrderCreated (이벤트) → wallet 잔고 잠금
  → OrderReady (이벤트) → OrderBook 등록
  → (background @Scheduled) 매칭 → TradeCreated (이벤트)
  → OrderMatched (이벤트) → 잔고 정산 (이벤트들 더)
  → 사용자에게 *언젠가* 결과
```

사용자는 *즉시 응답*을 받아야 하는데 multi-hop 비동기 체인으로 풀고 있었다.
그리고 이 모든 흐름이 *우리 시스템 안*에서 일어난다 — 외부 시스템과의 경계가 아니다.
한 트랜잭션으로 끝낼 수 있는 일을 굳이 분산 패턴으로 풀고 있었던 셈이다.

Outbox 패턴, 컨슈머 멱등성, SAGA 보상까지 다 갖춰도 결국 *eventual consistency*다. 사용자 입장에서 "내 잔고 차감됐나"의 답을 *언젠가* 주는 시스템.
하지만 이건 *우리가 통제할 수 있는 일*이고, *동기로 풀면 즉시 답이 나오는 일*이다.

## 동기가 맞는 경계 / 이벤트가 맞는 경계

깨달음 직후 정리한 기준:

| 동기가 맞는 경우 | 이벤트가 맞는 경우 |
|---|---|
| 사용자가 즉시 결과를 알아야 함 | side-effect (감사/알림/통계) |
| 정합성 깨지면 돈/법적 문제 | 일시적 불일치가 무해 |
| 한 트랜잭션 안에서 끝남 | 외부 시스템 경계 (필연적 분산) |
| 호출 빈도가 낮음 | 호출 빈도가 매우 높음 |

이 기준으로 우리 도메인을 다시 분류해 보면:

- **매수/매도 잔고 잠금 + 매칭 + 정산** → 왼쪽. 사용자 즉시 응답 + 우리 시스템 안 + 정합성 핵심
- **입금/출금 승인** → 왼쪽. 어드민이 승인하는 즉시 잔고 반영되어야 함
- **알림** → 오른쪽. 실패해도 사용자 경험에 치명적이지 않음
- **체결 기록 (감사용)** → 오른쪽. 다운스트림 컨슈머 (시세 피드, 회계)
- **롤백/실패 보상** → 오른쪽. 그 자체가 비동기 보상 흐름

지금까지 *전부 오른쪽으로* 풀고 있었다는 게 보였다. 5단계의 진짜 작업이 *Kafka로 갈아끼우기*가 아니라 **적재적소 재조정**이라는 게 명확해졌다.

## 지금 손보는 이유

깨달음을 *회고에만 적고 구조는 그대로 두는* 선택지도 있었다. 하지만 곧 마이그레이션 비용을 따져보니 *지금 손대는 게 훨씬 싸다*는 결론이 나왔다.

만약 5단계를 원래 계획대로 끝낸다고 가정하면:
- Outbox 테이블이 publisher 4개 서비스에 박혀 있고
- Kafka 토픽 17개에 매일 메시지가 흐르고
- 컨슈머마다 ProcessedEvent dedup 테이블이 누적되고
- 5단계 회고 문서가 *"분산 정확성을 갖춘 이벤트 기반 시스템"* narrative로 쓰여 있고

이 상태에서 *"사실 동기가 맞았다"*로 갈아엎는 비용은 5단계를 통째로 다시 만드는 것에 가깝다.
지금 단계에서 손대면 — 1주차 작업의 일부 *방향만 바꾸는* 것으로 끝난다.

또 portfolio narrative 측면에서도, *"EDA에서 시작해서 적재적소로 다시 잡은 흔적"*이 *"끝까지 EDA를 밀어붙인 흔적"*보다 더 깊은 시스템 사고를 보여준다고 판단했다. 깨달음만 적고 코드는 안 바꾸는 건 *말로만 한 변화*에 가깝다.

## 한 일 1 — funds-service에 internal RPC

먼저 잔고 조작을 *동기 호출*로 가능하게 하는 internal API를 funds-service에 만들었다.

```
POST /internal/funds/krw/credit   { userId, amount }   ← 입금/매도 정산
POST /internal/funds/krw/debit    { userId, amount }   ← 매수 잠금/출금
POST /internal/funds/coin/credit  { userId, coinId, amount }   ← 매수 체결
POST /internal/funds/coin/debit   { userId, coinId, amount }   ← 매도 잠금
POST /internal/funds/settle       { buyerId, sellerId, coinId, matchedAmount, totalKrw }
```

`/settle`이 흥미로운 결정이었다. 매칭이 체결되면 *buyer 코인 증가 + seller KRW 증가*가 한꺼번에 일어나야 하는데, 두 번의 sync 호출로 풀면 *부분 성공*이 가능해진다.
하지만 둘 다 같은 funds-db에 있으므로, funds-service 안에서 *한 `@Transactional`* 메서드로 묶으면 atomic이 보장된다.
이런 *원자성 묶음*은 SAGA로 풀 일이 아니라 *서비스 내부 한 트랜잭션*으로 풀어야 한다는 점이 다시 확인됐다.

경로 prefix는 4단계에서 정한 *외부/내부 분리* 컨벤션을 그대로 따랐다. `/internal/**`은 gateway 라우팅에서 제외되어 docker 내부망에서만 접근 가능 — 외부 노출 위험이 없다.

## 한 일 2 — trading-service의 sync 흐름

매수/매도의 흐름을 *이벤트 체인*에서 *한 메서드 안의 sync 시퀀스*로 옮겼다.

새 모델:
```
OrderController (@CurrentUserId)
  ↓
OrderFlowService.placeBuyOrder  ← @Transactional
  ├─ fundsClient.debitKrw       ← sync RPC to funds-service
  ├─ orderService.createBuyOrder (Order 저장)
  ├─ orderBookService.placeOrder (Redis)
  ├─ matchingEngine.match()     ← Redis Lua, sub-ms
  └─ for each match:
       ├─ tradeService.createTrade
       ├─ fundsClient.settle    ← sync RPC, 양쪽 정산 atomic
       ├─ orderService.fillOrder × 2
       └─ NotificationRequested 이벤트 (← 여기는 비동기 유지)
```

가장 큰 구조적 변화 — `MatchingEngineServiceWithRedis`의 `@Scheduled(500ms)`를 제거했다.
이전 모델은 *주문이 들어왔는지와 무관하게* 0.5초마다 매칭을 시도했는데, 사실 매칭이 일어날 수 있는 시점은 *새 주문이 들어왔을 때*뿐이다. 새 주문 없이 OrderBook이 가만히 있으면 매칭될 일도 없다.
그래서 *주문 진입 시점에서 트리거*하는 것으로 충분 — 동시에 *훨씬 단순한 모델*이 된다.

`OrderFlowService`는 trading-service의 composition root에 있다. 도메인 모듈(`domain-order`, `domain-trade`)은 자기 엔티티 CRUD만 책임지고, *여러 도메인을 가로지르는 orchestration*은 composition root에 두는 layering. 이전 챕터들의 패턴과 일치.

## 한 일 3 — 사라진 코드

sync 전환과 함께 *대거 dead code*가 됐다. 약 800줄 삭제.

**trading-service에서 사라진 것**:
- `BuyOrderReadyEventListener`, `SellOrderReadyEventListener` — sync로 풀어서 불필요
- `OrderMatchedEventListener`, `OrderProcessingFailedEventListener` — sync 응답으로 처리
- `BuyOrder*EventHandler`, `SellOrder*EventHandler`, `OrderMatched/TradeCreatedEventHandler` 8개 — Spring 이벤트→RabbitMQ 브릿지 더 이상 필요 없음
- `Order` 엔티티의 `@DomainEvents` + `BuyOrderFilledEvent` 등 자동 발행 로직 — fill()의 side-effect 제거
- `MatchingEngineServiceWithRedis`의 `@Scheduled` + `TradeCreatedEvent` 발행

**funds-service에서 사라진 것**:
- `BuyOrderCreatedEventListener`, `SellOrderCreatedEventListener`, `BuyOrderFilledEventListener`, `SellOrderFilledEventListener`, `BuyOrderCompletedEventListener`, `SellOrderCompletedEventListener` — 6개 listener
- `BuyOrderReadyEventHandler`, `SellOrderReadyEventHandler`, `OrderProcessingFailedEventHandler` — 3개 publisher
- `WalletService.processBuyOrder`, `processSellOrderFill`, `CoinWalletService.processSellOrder`, `processBuyOrderCompletion` 등 — sync API와 중복되는 메서드들

복잡도가 *눈에 보이게* 줄었다. *원래 단순했던 흐름을 분산 패턴으로 복잡하게 풀고 있었음*이 코드 줄 수로도 드러난다.

## 부산물 — 인증 정리

5단계 작업 중 인증 관련도 한 번 더 정리했다 (4단계의 마무리에 가까움):

- 4단계에서 `app/` 모듈을 삭제하면서 `WebConfig`도 같이 사라졌었다 — `@CurrentUserId` resolver가 등록 안 되어 있던 상태였음. 다행히 K6는 `/api/orders/*`에서 하드코딩 userId를 썼기 때문에 안 깨졌었다.
- `CurrentUserIdArgumentResolver`를 cookie + JwtTokenProvider 기반에서 *SecurityContext 기반*으로 바꿨다. 4단계에서 gateway가 토큰 검증 후 X-User-Id 헤더를 박고, downstream의 `HeaderAuthenticationFilter`가 SecurityContext에 그걸 박아 두는 흐름이 있으므로, resolver는 그걸 읽기만 하면 된다. 깔끔.
- 새 `WebMvcSecurityConfig`를 common-core에 두어 모든 서비스가 공유하도록 했다.

## 한 일 4 — 입금/출금 승인도 sync로

매수/매도와 같은 패턴으로, `deposit-service`와 `withdraw-service`의 *승인* 흐름도 sync RPC로 바꿨다.

승인은 어드민이 클릭하는 즉시 잔고에 반영되어야 하는 *정합성 도메인* — 사용자 입장에서 "내 입금 처리됐나"의 답을 즉시 받아야 한다. 이전엔 `DepositApprovedEvent` → 큐 → 컨슈머 → 잔고 증가의 비동기 체인이었는데, 동기로 직접 호출:

```
DepositAdminController
  ↓ (admin 권한 체크)
DepositApprovalService.approve  ← @Transactional
  ├─ deposit.approve() (Deposit 도메인)
  ├─ walletService.creditKrw (직접 호출 — 같은 funds-service 안)
  └─ NotificationRequestedEvent (비동기, Kafka)
```

`WalletService`를 같은 서비스(`funds-service`) 내부에서 직접 부르는 패턴 — RPC 거치지 않고 in-process. `OrderFlowService`가 `FundsClient`로 *교차 서비스* 호출인 것과 대비. *분리는 서비스 경계에서만 하고, 내부에선 직접 호출*이 합리적.

`DepositApprovedEventHandler`, `DepositApprovedEventListener`, `WithdrawApprovedEventHandler`, `WithdrawApprovedEventListener`, `WithdrawFailedEventListener` 모두 dead code가 되어 삭제. `WithdrawFailedEvent`도 더 이상 발행되지 않는다 — sync 흐름에서 잔고 차감 실패는 트랜잭션 롤백으로 끝남.

거절 흐름(`DepositRejectedEvent`, `WithdrawRejectedEvent`)은 *알림 트리거*라 비동기 그대로 유지.

## 한 일 5 — Kafka + Outbox 도입

남은 *진짜 비동기 이벤트들*에만 Kafka를 적용했다. 적용 대상:
- `NotificationRequestedEvent` (체결/입출금 알림)
- `DepositRejectedEvent`, `WithdrawRejectedEvent` (거절 알림)

토픽 이름: `notification.requested`, `deposit.rejected`, `withdraw.rejected`. 파티션 수 **8**, 키 = userId. 6단계 수평 확장 시 사용자 단위 병렬 처리 + 같은 사용자의 알림 순서 보장.

### Outbox 패턴

발행 보장을 위해 outbox 패턴을 도입. 비즈니스 트랜잭션과 publish 의도를 *같은 DB 트랜잭션 안에서* 원자적으로 커밋.

```
[publisher 측 — funds-service / trading-service]
  비즈니스 @Transactional {
      DB 변경 (잔고/주문/거래)
      ApplicationEventPublisher.publishEvent(NotificationRequestedEvent)
  }
  ↓
  EventToOutboxBridge (BEFORE_COMMIT)  ← 같은 트랜잭션 안
      outbox_message INSERT
  ↓
  COMMIT
  ↓
[OutboxRelay (별도 스케줄)]
  @Scheduled(200ms) + @Transactional
      SELECT FOR UPDATE limit 200
      KafkaTemplate.send().get()
      mark PUBLISHED
```

`@TransactionalEventListener(BEFORE_COMMIT)`이 핵심 — 리스너가 비즈니스 트랜잭션 안에서 실행돼서, outbox INSERT가 원자적으로 묶임. 비즈니스 변경이 롤백되면 outbox 행도 같이 사라진다 (publish가 일어나지 않을 변경은 publish하지 않음).

처음엔 *push + polling 하이브리드*로 시도했다 — `AFTER_COMMIT`에서 인메모리 신호를 보내 50ms 스케줄러가 즉시 깨우게 하고, 1초 폴링은 안전망으로 두는 형태. 하지만 50ms 스케줄러와 1초 스케줄러가 동시에 같은 `SELECT ... FOR UPDATE NOWAIT`를 실행하면서 락 경합이 생겨, *애초에 NOWAIT를 쓴 의도(타 인스턴스와 안전 분배)*와 *현재 단일 인스턴스에서의 자기 충돌*이 섞여 망가졌다. 단순화 — push 트리거 빼고 200ms 폴링만, NOWAIT 빼고 일반 락 대기로.

### Outbox 도입의 OrderBook race

매수/매도 흐름에서 한 함정 — `OrderFlowService.placeBuyOrder`에 `@Transactional`이 걸려 있으면, *Order 저장 → OrderBook(Redis) 등록 → 매칭 → 정산*이 한 트랜잭션 안. Order는 트랜잭션 끝에 commit되는데 *OrderBook(Redis)은 즉시 등록*. 두 자원이 트랜잭셔널 묶음이 아니다.

다른 스레드의 매칭 시점에 OrderBook에선 보이는데 DB에선 아직 안 보이는 *간극*이 생긴다. 그 시점에 매칭이 잡히면 `fillOrder(otherOrderId)`가 *commit 전 다른 트랜잭션*의 row를 못 찾아서 `ORDER_NOT_FOUND` 폭발.

수정 방향:
- `placeBuyOrder`/`placeSellOrder`의 `@Transactional` 제거
- `orderService.createBuyOrder`만 @Transactional (자체 커밋) → OrderBook 등록은 **commit 후**
- `processMatch`는 별도 빈(`MatchProcessor`)으로 추출 — self-invocation 회피

```java
public void placeBuyOrder(...) {
    fundsClient.debitKrw(...);
    Order order = orderService.createBuyOrder(...);  // @Transactional 안에서 commit
    orderBookService.placeOrder(order);              // commit 후에 OrderBook
    for (match : matchingEngine.match()) {
        matchProcessor.processMatch(match);          // 각 매칭이 별도 @Transactional
    }
}
```

이 race는 *Outbox 도입과 무관*하게 잠재해 있던 결함이었다. 이전 K6 시나리오는 부하가 낮을 때 우연히 안 터졌을 뿐. Outbox 도입으로 트랜잭션 path가 더 길어지면서 (BEFORE_COMMIT 핸들러 + 추가 INSERT) 노출됐다.

## 검증

K6 회귀: **8,083 요청 / 0 에러, p95 2.5s, throughput 100/s**.

이전 단계(4단계 gateway 도입 후) 대비:
| 단계 | 처리량 | p95 | 에러 |
|---|---|---|---|
| 4단계 (gateway) | 449/s | 494ms | 0% |
| **5단계 (sync + Kafka + Outbox)** | **100/s** | **2.5s** | **0%** |

응답 시간이 5배, 처리량이 4배 떨어졌다. 이게 *비싸 보이는데*, 이번 챕터에서 추가된 정합성/내구성 비용을 정량화한 결과:

- sync RPC: 매수 흐름에 `funds.debitKrw` + `funds.settle` 두 번의 HTTP hop 추가
- Outbox INSERT: 모든 이벤트 발행에 DB INSERT 한 번 추가
- Outbox 폴링: 200ms 주기로 SELECT FOR UPDATE — 다른 INSERT와 락 경합
- Kafka 발행: relay → Kafka send (sync `.get()`)

각 비용은 작지만 한 요청에 4~5번 곱해지면서 누적. 이 *측정값*은 7단계 운영 이슈에서 풀어나갈 출발점이 된다 (HikariCP 풀 튜닝, Kafka producer batching, outbox 폴링 주기).

파티션 키 동작 검증:
- `notification.requested` 토픽: 8 파티션 자동 생성 ✓
- `userId` 키 기반 라우팅 — 같은 사용자는 같은 파티션으로 흐름

## 의도적으로 미룬 것

이 챕터에서 *원래 계획에 있었지만 7단계로 미룬 것* 두 가지:

**1. 컨슈머 멱등성 (`ProcessedEvent` dedup 테이블)**

Outbox는 *최소 한 번* 보장 — relay가 발행 후 마킹 전에 죽으면 재발행됨. 컨슈머에서 중복 처리되면 같은 거래가 두 번 생성되는 식의 정합성 깨짐. 이걸 막으려면 `eventId` 기반 dedup이 필요.

본 챕터에서 빼둔 이유:
- 우리 K6 시나리오에선 컨슈머 리밸런스/재시도가 발생하지 않음 — 단일 컨슈머 인스턴스
- 6단계 k8s + HPA에서 컨슈머 인스턴스가 늘어나면 진짜 문제로 만남
- 그때 `eventId` 추가 + `processed_event` 테이블을 한 번에 묶어서 도입하는 게 *문맥과 같이 가는* 정리

**2. 이벤트에 `eventId` 필드 추가**

위 멱등성과 짝. eventId 없이 dedup 못 하므로 같이 미룸.

이 두 가지는 7단계 *운영 이슈* 챕터에서 회로차단기, 분산 트레이싱과 함께 다룬다.

## 한 줄 정리

이 챕터의 본질은 *"Kafka로 갈아끼우기"*가 아니라 **"이벤트 만능에서 적재적소로 — 정합성 도메인을 sync로 되돌리고, 진짜 비동기인 것들만 Kafka 위에 Outbox로 보장한다"**.

이 깨달음을 *코드까지 적용한* 시점이 6단계(k8s/HPA, 자원 격리) 들어가기 직전이라는 게 결정적이었다 — 더 늦으면 마이그레이션 비용이 비례 이상으로 증가한다.

응답 시간 저하가 의미 있는 수준이지만, 그건 *정합성을 사기 위한 비용*. 7단계의 튜닝 출발점.
