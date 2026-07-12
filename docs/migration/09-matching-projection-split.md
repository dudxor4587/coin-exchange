8단계에서 주문을 durable 로그로 옮기면서, trading 프로세스는 두 가지 일을 같이 하고 있었다. <br>
하나는 hot path — 주문을 받아 Redis OrderBook에 넣고 매칭하는 것이고, 다른 하나는 Kafka 로그를 읽어 DB에 반영하는 projection 컨슈머다. <br>
그리고 매칭 코드가 들어 있는 `domain-order` 모듈 안에는 DB의 `Order` 엔티티도 같이 있었다. <br>
매칭엔진이 DB를 알 이유가 없는데, 코드도 프로세스도 그 둘이 붙어 있었다. 이 챕터는 그걸 떼어낸다. <br>

# 무엇이 붙어 있었나
두 층위에서 결합이 있었다. <br>

1. **모듈** — `domain-order` 안에 Redis 매칭(`OrderBookService`, `OrderBook`, `RedisOrderBookService`)과 DB(`Order`, `OrderService`)가 같이 들어 있었다. 게다가 `OrderBookService.placeOrder`가 DB 엔티티인 `Order`를 파라미터로 받았고, `OrderBook`은 `@Entity`였다. 매칭에 필요한 건 주문의 몇 개 필드뿐인데 DB 타입에 묶여 있었다.
2. **프로세스** — trading 하나가 hot path(매칭)와 projection 컨슈머(DB 쓰기)를 같이 돌렸다. 매칭은 Redis만 있으면 되는데, 같은 프로세스가 DB 커넥션과 Kafka 컨슈머까지 들고 있었다.

# 매칭엔진에서 DB 떼기 — domain-matching 모듈
`domain-order`를 둘로 갈랐다. <br>
- **domain-matching** — Redis 매칭만. `OrderBookService`, `RedisOrderBookService`, `MatchingEngineServiceWithRedis`, `RedisOrderIdGenerator`, `matchingLogic.lua`.
- **domain-order** — DB만. `Order`, `OrderService`, `OrderRepository`.

떼면서 두 가지를 바꿨다. <br>
1. **`OrderBook`을 POJO로.** `@Entity`를 떼고 그냥 객체로 만들었다. OrderBook은 Redis 해시로만 저장되니 JPA 엔티티일 이유가 없었다. 이걸로 domain-matching에서 DB 의존이 사라졌다.
2. **`OrderBookService.placeOrder`를 raw 필드로.** `Order` 엔티티 대신 orderId·coinId·price·amount·side·userId를 직접 받게 했다. 매칭엔진이 더는 DB 타입을 모른다.

같이 정리한 것도 있다. 5단계 동기 전환 때 죽었던 SAGA 매칭/롤백 코드(`JpaOrderBookService`, `MatchingEngineService`, 롤백 핸들러들)가 그대로 남아 있어서 삭제했다. 발행하는 곳이 없는 이벤트와 핸들러였다. <br>

# 컨슈머를 별도 서비스로 — app-projection
projection 컨슈머를 trading에서 들어내 별도 서비스로 뺐다. <br>

우선 producer와 consumer가 공유하던 Kafka 규약(토픽 이름, eventType 헤더)을 events-contract의 `OrderLogChannel`로 올렸다. 두 서비스가 같은 계약을 봐야 하기 때문이다. <br>
그다음 컨슈머 쪽 코드 — `OrderLogConsumer`, `OrderProjectionService`, dedup(`ProcessedEvent`), 알림 브릿지 — 를 새 `app-projection` 서비스로 옮겼다. <br>

그 결과 trading에는 hot path만 남았다. <br>
```
[trading]     Redis(매칭) + Kafka(로그 append) + funds RPC(정산)   ← DB projection 없음, RabbitMQ 없음
[projection]  Kafka(로그 소비) → DB(Order/Trade) + 알림
```
trading은 이제 주문 DB에 쓰지 않는다(coin 시드용 DB 연결만 남았다). 알림도 projection이 발행하니 trading에서 RabbitMQ도 빠졌다. <br>

# 처리량은 분리의 이유가 아니었다
분리하기 전에는, 컨슈머가 trading 안에서 같이 돌며 자원을 나눠 쓰니 떼면 trading 처리량이 오를 거라 예상했다. <br>
그래서 컨슈머를 trading 안에 둔 경우와 별도 서비스로 뺀 경우를 같은 조건에서 측정했다. <br>

| | 컨슈머 in trading | 컨슈머 in projection |
|---|---|---|
| 처리량 | 110/s | 107/s |
| p95 | 2.48s | 2.63s |

차이가 없었다(오차범위). 컨슈머를 어디에 두든 trading의 hot path 처리량은 같았다. <br>
예상과 달랐는데, hot path 처리량은 컨슈머와 경쟁하는 CPU가 아니라 동기 I/O 왕복(6~8단계에서 계속 만난 그 천장)에 묶여 있기 때문이다. 컨슈머를 옆에서 치워도 그 천장은 그대로다. <br>

그러니 이 분리는 처리량 최적화가 아니다. 이유는 다른 데 있다. <br>
1. **관심사 분리** — 매칭엔진이 DB를 모른다. Redis 매칭과 DB projection이 코드와 프로세스 양쪽에서 갈렸다.
2. **독립 배포·확장** — projection은 trading과 무관하게 따로 배포하고, 필요하면 따로 늘릴 수 있다. 다만 로그 토픽이 단일 파티션이라, projection 컨슈머의 병렬 확장은 순서 보장과 트레이드오프다(8단계에서 정한 대로).

# 정리
> 8단계 뒤 trading은 매칭(Redis)과 projection(DB 컨슈머)을 한 프로세스·한 모듈에서 같이 들고 있었다. <br>
> 매칭엔진을 domain-matching으로 떼어 DB를 모르게 하고(OrderBook은 POJO, placeOrder는 raw 필드), 컨슈머를 app-projection 서비스로 옮겨 trading을 Redis+Kafka+RPC로 좁혔다. <br>
> 처리량이 오를 줄 알고 측정했지만 컨슈머 위치는 처리량과 무관했다(110 vs 107). 이 분리의 값은 처리량이 아니라 관심사 분리와 독립 배포에 있다. <br>
