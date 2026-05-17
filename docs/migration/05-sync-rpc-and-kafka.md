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
  └─ NotificationRequestedEvent (비동기, Kafka)
```

`WalletService`를 같은 서비스(funds-service) 내부에서 직접 부르는 패턴이다 — RPC 거치지 않고 같은 프로세스 안에서. <br>
`OrderFlowService`가 `FundsClient`로 *교차 서비스* 호출인 것과 대비된다. <br>
*분리는 서비스 경계에서만 하고, 내부에선 직접 호출* 하는 게 합리적이라는 원칙이 다시 확인된다. <br>

`DepositApprovedEventHandler`, `DepositApprovedEventListener`, `WithdrawApprovedEventHandler`, `WithdrawApprovedEventListener`, `WithdrawFailedEventListener` 모두 dead code가 되어 삭제했다. <br>
`WithdrawFailedEvent`도 더 이상 발행되지 않는다 — 동기 흐름에서 잔고 차감 실패는 트랜잭션 롤백으로 끝나기 때문이다. <br>

거절 흐름(`DepositRejectedEvent`, `WithdrawRejectedEvent`)은 *알림 트리거* 라 비동기로 유지했다. <br>

# 한 일 5 — Kafka + Outbox 도입
남은 *진짜 비동기 이벤트들* 에만 Kafka를 적용했다. <br>
- `NotificationRequestedEvent` (체결/입출금 알림)
- `DepositRejectedEvent`, `WithdrawRejectedEvent` (거절 알림)

토픽 이름은 `notification.requested`, `deposit.rejected`, `withdraw.rejected`. <br>
파티션 수는 **8**, 키는 userId. <br>
6단계 수평 확장 시 사용자 단위 병렬 처리 + 같은 사용자의 알림 순서 보장을 위해서다. <br>

## Outbox 패턴
발행 보장을 위해 outbox 패턴을 도입했다. <br>
비즈니스 트랜잭션과 발행 의도를 같은 DB 트랜잭션 안에서 원자적으로 커밋하는 구조다. <br>

```
[publisher 측 — funds-service / trading-service]
  비즈니스 @Transactional {
      DB 변경 (잔고/주문/거래)
      ApplicationEventPublisher.publishEvent(NotificationRequestedEvent)
  }
  ↓
  EventToOutboxBridge (BEFORE_COMMIT)  ← 같은 트랜잭션 안
      outbox_message INSERT + OutboxInsertedSignal 발행
  ↓
  COMMIT
  ↓
[OutboxRelay (별도 스레드)]
  AFTER_COMMIT 시그널로 깨어남 + 1초 안전망 폴링
      SELECT FOR UPDATE limit 200
      KafkaTemplate.send().get()
      mark PUBLISHED
```

`@TransactionalEventListener(BEFORE_COMMIT)`이 핵심이다 — 리스너가 비즈니스 트랜잭션 안에서 실행돼서, outbox INSERT가 원자적으로 묶인다. <br>
비즈니스 변경이 롤백되면 outbox row도 같이 사라진다 (발행이 일어나지 않을 변경은 발행하지 않는다). <br>

처음엔 *push + polling 하이브리드* 를 시도했었다. <br>
`AFTER_COMMIT`에서 인메모리 신호를 보내 50ms 스케줄러가 즉시 깨우게 하고, 1초 폴링은 안전망으로 두는 형태였다. <br>
하지만 50ms 스케줄러와 1초 스케줄러가 동시에 같은 `SELECT ... FOR UPDATE NOWAIT`를 실행하면서 락 경합이 생겼다. <br>
*애초에 NOWAIT를 쓴 의도(다른 인스턴스와의 안전 분배)* 와 *현재 단일 인스턴스에서의 자기 자신과의 충돌* 이 섞여 망가졌다. <br>

해결은 워커 하나로 줄이고 *신호 + 타임아웃 안전망* 이었다. <br>
백그라운드 스레드 하나가 Semaphore 시그널로 깨어나거나 1초 타임아웃으로 깨어나는 형태로 단순화했다. <br>
자세한 설계 결정은 [발행 보장 (Transactional Outbox)](../발행%20보장%20\(Transactional%20Outbox\).md) 문서에 정리했다. <br>

## Outbox 도입에서 만난 OrderBook race
매수/매도 흐름에서 함정이 하나 있었다. <br>
`OrderFlowService.placeBuyOrder`에 `@Transactional`이 걸려 있으면, *Order 저장 → OrderBook(Redis) 등록 → 매칭 → 정산* 이 한 트랜잭션 안에 묶인다. <br>
Order는 트랜잭션 끝에 commit되는데 *OrderBook(Redis)은 즉시 등록* 된다. <br>
두 자원이 트랜잭션으로 묶이지 않는다. <br>

다른 스레드의 매칭 시점에 OrderBook에는 보이는데 DB에는 아직 안 보이는 *간극* 이 생긴다. <br>
그 시점에 매칭이 잡히면 `fillOrder(otherOrderId)`가 *commit 전 다른 트랜잭션* 의 row를 못 찾아서 `ORDER_NOT_FOUND`가 터진다. <br>

수정 방향은 다음과 같다.
1. `placeBuyOrder`/`placeSellOrder`의 `@Transactional` 제거.
2. `orderService.createBuyOrder`만 `@Transactional` (자체 commit) → OrderBook 등록은 commit 후.
3. `processMatch`는 별도 빈(`MatchProcessor`)으로 추출 — 자기 자신 호출 회피.

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

이 race는 *Outbox 도입과 무관하게* 잠재해 있던 결함이었다. <br>
이전 K6 시나리오는 부하가 낮을 때 우연히 안 터졌을 뿐이다. <br>
Outbox 도입으로 트랜잭션 path가 더 길어지면서 (BEFORE_COMMIT 핸들러 + 추가 INSERT) 드러난 셈이다. <br>

# 한 일 6 — 컨슈머 멱등성도 같이 도입
원래는 컨슈머 멱등성을 7단계로 미루려 했었다. <br>
*우리 K6 시나리오는 단일 컨슈머 인스턴스라 리밸런스가 안 일어나니까* 라는 이유였는데, 복기하다가 이 판단이 *틀렸음* 을 깨달았다. <br>

리밸런스가 없어도 *프로세스 크래시 타이밍* 만으로 중복은 발생한다.
1. 릴레이가 Kafka에 보낸 후 `markPublished` commit 전 크래시 → 같은 메시지 재발행
2. 컨슈머가 비즈니스 처리 후 offset commit 전 크래시 → 같은 메시지 재처리

즉 *단일 인스턴스에서도 운영 중에는 결국 발생할* 정확성 위험이었다. <br>
측정 후 결정할 사안이 아니라 *이미 알고 있는 문제* 였다. <br>
다른 미룬 항목들(async batch, 다중 인스턴스 락 전략 등)이 *측정 후 결정* 이었던 것과 본질적으로 다른 종류라, 5단계 마무리에 포함시키기로 했다. <br>

도입한 것:
1. 이벤트에 `eventId` (UUID) 필드 + 편의 생성자 — 기존 호출부 변경 없이 자동 생성.
2. `infra-notification`에 MySQL DB 추가 + `processed_event` 테이블.
3. 3개 컨슈머 모두 `@Transactional` + dedup 체크 패턴.

```java
@KafkaListener(...)
@Transactional
public void handle(String json) {
    Event event = objectMapper.readValue(json, ...);
    if (processedEventRepository.existsById(event.eventId())) return;

    notificationSender.send(...);
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
}
```

비즈니스 변경과 dedup INSERT가 같은 트랜잭션에 묶여서, *비즈니스만 됐는데 dedup 마커는 없음* 같은 상태가 구조적으로 불가능해진다. <br>

다만 한계도 있다. <br>
`notificationSender.send()` 같은 *외부 시스템 호출* 은 여전히 트랜잭션 밖이라, send → commit 사이 윈도우에 크래시하면 외부 호출은 중복될 수 있다. <br>
*DB 변경은 한 번만, 외부 호출은 최소 한 번* 까지가 우리가 보장할 수 있는 한계다. <br>
완전한 한 번 보장은 외부 시스템 측에서도 멱등성을 보장해줘야 가능한데, 그건 분산 시스템의 본질적 한계라 *발행 + 컨슈머 측 dedup* 까지가 우리 책임의 끝이다. <br>

# 검증
K6 회귀를 두 단계로 나눠 측정했다. <br>
sync RPC + Outbox + Kafka 까지만 도입한 *중간 시점* 과, 컨슈머 멱등성까지 도입한 *최종 시점* 의 비용을 분리해서 보기 위해서다. <br>

| 단계 | 처리량 | p95 | 에러 |
|---|---|---|---|
| 4단계 (gateway) | 449/s | 494ms | 0% |
| 5단계 중간 (sync RPC + Outbox + Kafka) | 100/s | 2.5s | 0% |
| **5단계 최종 (+ 컨슈머 멱등성)** | **62/s** | **4.8s** | **0%** |

4단계 대비 응답 시간이 약 10배, 처리량이 약 7배 떨어졌다. <br>
이게 비싸 보이는데, 이번 챕터에서 추가된 정합성/내구성 비용을 정량화한 결과는 다음과 같다.
1. 동기 RPC — 매수 흐름에 `funds.debitKrw` + `funds.settle` 두 번의 HTTP hop 추가
2. Outbox INSERT — 모든 이벤트 발행에 DB INSERT 한 번 추가
3. Outbox 폴링 — SELECT FOR UPDATE, 다른 INSERT와 락 경합
4. Kafka 발행 — relay → Kafka send (sync `.get()`)
5. dedup 체크 — 컨슈머마다 SELECT + INSERT 한 번씩 추가 (notification-db 인스턴스도 신규)

각 비용은 작지만 한 요청에 4~5번 곱해지면서 누적된다. <br>
중간 → 최종 사이의 추가 비용(처리량 100 → 62/s) 은 거의 전부 *컨슈머 멱등성* 에서 왔다. notification 컨슈머가 메시지마다 SELECT + INSERT 를 추가로 하기 때문이다. <br>
이 *측정값* 은 7단계 운영 이슈에서 풀어나갈 출발점이 된다 (HikariCP 풀 튜닝, Kafka 비동기 발행, outbox 폴링 주기, processed_event 인덱스 등). <br>

파티션 키 동작 검증:
- `notification.requested` 토픽: 8 파티션 자동 생성 ✓
- userId 키 기반 라우팅 — 같은 사용자는 같은 파티션으로 흐름 ✓
- 3개 토픽 모두 1 컨슈머에 8 파티션씩 정상 할당 ✓

# 의도적으로 미룬 것
이 챕터에서 원래 계획에 있었지만 7단계로 미룬 것들:
- 다중 인스턴스 락 전략 (NOWAIT vs SKIP LOCKED) — 다중 인스턴스로 띄워봐야 측정 가능
- 비동기 Kafka 발행 — 진짜 병목인지 측정 후 결정
- 다중 릴레이 — 비동기 발행 적용 후에도 부족하면 그때
- retry topic / DLQ / 회로차단기 — 운영 단계의 내구성 영역

각 항목의 측정 계획은 [발행 보장 — 미뤄둔 결정들](../발행%20보장%20—%20미뤄둔%20결정들.md) 문서에 정리했다. <br>

# 결론
> 이 챕터의 본질은 *"Kafka로 갈아끼우기"* 가 아니라 **"이벤트 만능에서 적재적소로 — 정합성 도메인을 동기로 되돌리고, 진짜 비동기인 것들만 Kafka 위에 Outbox로 보장한다"** 이다. <br>
> 이 깨달음을 *코드까지 적용한* 시점이 6단계(k8s/HPA, 자원 격리) 들어가기 직전이라는 게 결정적이었다. 더 늦으면 마이그레이션 비용이 비례 이상으로 증가한다. <br>
> 응답 시간 저하가 의미 있는 수준이지만, 그건 *정합성을 사기 위한 비용* 이다. 7단계의 튜닝 출발점이 된다.
