6단계에서 병목을 측정으로 규명했다. 주문 흐름에서 매칭은 0.1%였고, 진짜 병목은 주문을 DB에 동기로 저장하는 createOrder였다(전체의 76%). <br>
그리고 그 병목은 복제(HPA)로 풀리지 않고 곧 공유 자원 천장에 막혔다. <br>
처리량을 끌어올리는 길은 병목을 복제하는 게 아니라 *병목 자체를 없애는 것*이라는 결론이 났고, 이 챕터가 그 작업이다. <br>

# 왜 동기 DB 쓰기가 병목이었나
매수 흐름은 대학생 때 만든 그대로였다. <br>
```
주문 요청 → createOrder (DB INSERT, 동기) → OrderBook(Redis) 등록 → 매칭 → 정산
```
주문을 DB에 저장하고 그 결과(auto-increment id)를 받아야 다음으로 넘어가는 구조라, 사용자는 *DB 쓰기가 끝날 때까지* 기다렸다. <br>
200 동시 요청이 커넥션 풀 10개를 두고 경쟁하면서, INSERT 한 줄이 커넥션 대기로 2.9초까지 늘어났다. <br>

그런데 코드를 다시 보니 두 가지가 눈에 들어왔다. <br>
1. **매칭은 이미 Redis만 쓴다.** 매칭 Lua 스크립트는 OrderBook(Redis)만 읽고 쓰며 remainingAmount를 직접 차감한다. DB를 건드리지 않는다.
2. **OrderBook(Redis)이 이미 작업 데이터를 다 갖고 있다.** coinId·price·type·remainingAmount·userId가 전부 Redis에 있다.

즉 *주문의 진실은 이미 Redis에 있었고*, DB의 Order는 그 결과를 기록하는 사본에 가까웠다. <br>
사본을 만드느라 hot path에서 동기로 기다리고 있던 셈이다. <br>

# 구조 변경 — Redis 우선, DB는 비동기 사본
DB 쓰기를 hot path에서 들어내고, 진실인 Redis만 동기로 다뤘다. <br>

```
[hot path — 동기]
  debitKrw (RPC, 돈)          ← 돈은 즉시 정합성이 필요해 동기 유지
  id = Redis INCR             ← DB 없이 채번
  OrderBook → Redis (진실)
  match (Redis Lua)
  체결당 settle (RPC, 돈)
  응답
       ↓ 이벤트 발행
[projection — 비동기, 단일 스레드]
  Order INSERT → Trade INSERT → fillOrder   ← DB는 사본
```

바꾼 것은 다음과 같다. <br>
1. **id 채번을 Redis INCR로.** `@GeneratedValue(IDENTITY)`를 없애고 애플리케이션이 채번한다. OrderBook 등록 시점에 DB 없이 id가 필요해졌기 때문이다.
2. **주문/거래/체결의 DB 쓰기를 projector로.** `OrderPlacedEvent`/`TradeExecutedEvent`를 발행하고, `OrderProjectionHandler`가 `@Async`로 DB에 반영한다.
3. **돈(debit/settle)은 hot path에 남겼다.** 돈은 벌어지면 안 되는 정합성이라, 이번 챕터의 범위를 *주문 저장만 비동기*로 좁혔다.

## 순서 보장 — 단일 스레드 executor
projection executor는 스레드를 하나로 고정했다. <br>
주문 INSERT(OrderPlacedEvent)가 그 주문의 체결 fill(TradeExecutedEvent)보다 반드시 먼저 실행되어야, fillOrder의 findById가 깨지지 않는다. <br>
발행 순서 = 큐 순서 = 처리 순서(FIFO)가 되도록 단일 스레드로 뒀다. <br>

## 함정 — assigned id와 Spring Data JPA
id를 애플리케이션이 채번하면서 함정을 하나 만났다. <br>
Spring Data JPA는 `save()`할 때 엔티티에 id가 있으면 *기존 행으로 보고 merge(SELECT 후 INSERT)*로 빠진다. <br>
매 주문마다 불필요한 SELECT가 붙는 것이다. <br>
`Persistable`을 구현하고 `isNew()`를 auditing 타임스탬프(`createdAt == null`) 기준으로 판별하게 해서, 새 주문이 곧바로 INSERT로 가도록 했다. <br>

# 측정
docker-compose에서 K6 회귀를 돌렸다. <br>

| | 이전 (동기 DB) | 이번 (비동기 projection) |
|---|---|---|
| 처리량 | ~50/s | 100/s |
| p95 | 4~8s | 2.8s |
| 에러 | 0% | 0% |

**처리량이 2배가 됐다.** hot path 구간에서 createOrder(2.9초, 76%)가 사라졌고, 이제 hot path는 돈 RPC(debit ~700ms)와 Redis 연산이 차지한다. <br>

흥미로운 건 *projector가 부하를 따라잡았다*는 점이다. <br>
K6가 채번한 8046건이 DB에도 8046건 그대로 반영됐고 유실이 없었다. <br>
같은 DB 쓰기인데, 200개 요청이 커넥션 풀을 두고 경쟁하며 하던 것을 단일 스레드가 경쟁 없이 순차로 처리하니 오히려 효율적이었다. <br>
*동기 I/O를 hot path에서 빼면 처리량이 오른다*는 6단계의 가설이 데이터로 확인된 셈이다. <br>

절대 수치(100/s)는 여전히 로컬 환경 천장 안이라 그대로 의미를 갖긴 어렵지만, *구조 변경 전후의 상대 비교(2배)*는 유효하다. <br>

# 남긴 한계 — 다음 챕터의 숙제
이 구조는 의도적으로 구멍을 하나 남겼다. <br>
projection 큐는 인메모리이고 진실은 Redis다. <br>
1. **내구성** — trading이 크래시하면 아직 반영 안 된 projection이 유실된다. Redis엔 주문이 있는데 DB엔 없는 상태가 될 수 있다.
2. **지속 한계** — 이번엔 단일 스레드 projector가 100/s를 따라잡았지만, 더 높은 부하나 더 느린 DB에서는 큐가 쌓인다.

*"진실을 Redis(인메모리)에 둔다"*는 것의 한계다. <br>
Redis가 죽으면 진실 자체가 사라진다. <br>
다음 챕터에서는 진실을 durable 로그로 옮겨(Redis는 작업 상태, 로그가 진실, DB는 projection), 이 구멍을 메운다. <br>
5단계에서 과하다고 되돌렸던 Kafka가, 이벤트 소싱의 로그 저장소로서 그때 정당화된다. <br>

# 결론
> 이 챕터는 6단계 측정의 후속이다. 측정으로 *"동기 DB 쓰기가 병목"*임을 확인했고, 이번에 그걸 *구조로 없앴다*. <br>
> 주문의 진실은 원래부터 Redis에 있었고, DB는 사본이었다. 사본을 hot path에서 동기로 기다리던 것을 비동기로 미루자 처리량이 2배가 됐다. <br>
> 다만 진실을 인메모리(Redis)에 둔 대가로 내구성 구멍이 남았고, 그것이 다음 챕터의 출발점이다.
