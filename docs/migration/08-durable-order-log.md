7단계에서 주문 저장을 hot path에서 들어내 처리량을 2배로 만들었다. <br>
그 구조는 의도적으로 구멍을 하나 남겼다 — projection 큐가 인메모리였다. <br>
trading이 크래시하면 아직 DB에 반영 안 된 주문이 유실된다. Redis엔 있는데 DB엔 없는 상태가 된다. <br>
이 챕터는 그 구멍을 메운다. <br>

# 7단계가 남긴 구멍
7단계의 진실은 Redis OrderBook이었고, DB로 가는 projection은 인메모리 큐(@Async)를 탔다. <br>
둘 다 프로세스가 죽으면 사라진다. <br>
주문이 접수됐다는 사실을 durable하게 적어둔 곳이 없었던 것이다. <br>
사용자에게 200을 돌려줬는데 그 주문이 유실될 수 있는 상태였다. <br>

# 구조 변경 — durable 로그를 진실로
주문이 일어났다는 사실을 디스크에 남는 로그(Kafka)에 먼저 적고, 그 다음에 나머지를 진행하게 했다. <br>

```
[hot path]
  debitKrw (RPC, 돈)
  id = Redis INCR
  OrderPlaced → Kafka append (동기, .get으로 저장 확인 대기)  ← 여기서 돌아오면 durable
  OrderBook → Redis (매칭용 작업 상태)
  match (Redis)
  체결당 settle (RPC, 돈) + TradeExecuted → Kafka append (동기)
  응답
       ↓
[컨슈머 — order-projection 그룹]
  OrderPlaced → Order INSERT
  TradeExecuted → Trade INSERT + fillOrder + 알림
  DB 반영 커밋 후 offset 커밋(manual ack)
```

역할이 세 층으로 갈렸다. <br>
1. **Kafka 로그 = 진실.** 무슨 주문/체결이 일어났는가의 durable한 원본.
2. **Redis OrderBook = 작업 상태.** 매칭이 빠르게 도는 곳. 로그에서 재구성 가능한 파생물.
3. **DB = 조회용 사본.** 컨슈머가 로그를 읽어 반영하는 projection.

## 동기 append
`kafkaTemplate.send(record).get()`으로 브로커의 저장 확인까지 기다린다. <br>
이 append에서 돌아왔다는 건 주문이 로그에 저장됐다는 뜻이고, 그때부터는 프로세스가 죽어도 유실되지 않는다. <br>
비동기(fire-and-forget)로 보내면 "보냈는데 저장 전 크래시" 윈도우가 남아 7단계의 구멍과 같아진다. 그래서 동기로 뒀다. <br>

## dedup
컨슈머는 DB 반영을 커밋한 뒤에 offset을 커밋한다. <br>
반영 후 커밋 전에 크래시하면 재시작한 컨슈머가 그 이벤트를 다시 읽는다(at-least-once). <br>
같은 이벤트가 두 번 반영되면 fillOrder가 두 번 더해져 잔고가 오염된다. <br>
이벤트마다 eventId를 두고, projection과 같은 트랜잭션에서 `processed_event`에 마커를 남겨 중복을 막았다. <br>

5단계에서 알림용으로는 과하다며 되돌렸던 Kafka와 컨슈머 dedup이, 주문 이벤트 소싱에서는 필요해 다시 들어왔다. <br>
되돌린 판단이 틀린 게 아니라, 알림에는 과했고 주문 로그에는 맞는 적재적소의 문제였다. <br>

# 크래시 복구
부하를 주는 도중 trading을 `docker kill`(SIGKILL, graceful 종료 없음)로 죽였다. <br>
kill 시점에 로그에는 있지만 DB에는 반영 안 된 주문이 수백 건 쌓여 있었다. 그리고 재시작했다. <br>

```
Kafka 총 로그: 6655 이벤트
컨슈머 lag: 0            → 재시작 후 전부 처리
processed_event: 6655   → 이벤트당 마커 1개
중복 order id: 0        → 재소비됐지만 dedup이 중복 차단
```

SIGKILL로 죽였는데 유실 0, 중복 0이었다. <br>
진실이 durable 로그에 있으니 재시작한 컨슈머가 offset부터 재개해 전부 반영했다. <br>
Kafka 리밸런스에 약 32초가 걸렸다. 복구가 즉시는 아니지만 완전하다. <br>

# 측정 — 내구성의 비용
| | 7단계 (인메모리) | 8단계 (Kafka durable) |
|---|---|---|
| 처리량 | 100/s | 54/s |
| p95 | 2.8s | 4.2s |
| 크래시 유실 | 있음 | 0 |

durability를 얻으니 처리량이 절반이 됐다. <br>

비용의 출처는 예상과 달랐다. <br>
동기 Kafka append 자체는 hot path 구간에서 5ms로 저렴했다. <br>
느려진 건 Redis 연산들(placeOrderBook 1671ms 등)이었고, Kafka 컨슈머(projection)가 trading 안에서 같이 돌며 생긴 부하가 공유 자원 경쟁을 늘린 탓이다. <br>
비용은 append 지연이 아니라 durable 로그 기계장치가 같은 컨테이너에서 도는 부하였다. <br>
컨슈머를 별도 서비스로 분리하면 trading 부하가 줄어 이 비용을 상당 부분 회수할 여지가 있다. 다음 개선의 자리다. <br>

절대 수치(54/s)는 여전히 로컬 환경 천장 안이라 그대로 의미를 갖긴 어렵고, 7→8단계의 상대 비교(2배 → 절반)가 내구성의 비용이다. <br>

# 결론
> 7단계가 처리량을 위해 남긴 내구성 구멍을 8단계가 durable 로그로 메웠다. <br>
> 진실을 인메모리(Redis)에서 디스크 로그(Kafka)로 옮기자 SIGKILL에도 주문이 유실되지 않았다(유실0/중복0). 대가로 처리량은 절반이 됐다. <br>
> 5단계에서 되돌렸던 Kafka와 dedup이 여기서 정당화됐다. 알림에는 과했고 주문 로그에는 맞았다 — 같은 기술을 두 번 측정해 자리를 맞춘 셈이다.
