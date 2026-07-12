서비스를 여러 개로 나누자 새 문제가 생겼다. **주문 하나가 여러 서비스에 흩어져 로그를 남긴다.** <br>
주문 한 건은 gateway→trading→funds(RPC)→Kafka→projection을 거친다. 무언가 잘못되면 이 중 어디서 실패했는지 6개 서비스의 로그를 각각 뒤져야 하고, 게다가 동시에 여러 주문이 처리되니 로그가 뒤섞여 **어느 줄이 같은 요청인지 구분조차 안 된다.** <br>
이 챕터는 요청마다 **traceId**를 붙이고 서비스를 넘어 전파해서, 로그를 한 요청 단위로 잇는다. <br>

# 문제 — 서비스가 갈라지니 요청을 못 쫓는다
단일 프로세스일 땐 스택 트레이스 하나에 요청의 전 과정이 담겼다. <br>
서비스가 갈라지자 그게 안 된다. 주문 하나의 로그가 trading·funds·projection에 나뉘어 찍히고, 각 서비스 로그만 봐선 그게 같은 주문인지 알 수 없다. <br>
동시 주문 100건이 들어오면 각 서비스 로그에도 100건이 뒤섞여 찍힌다. "이 주문이 왜 느렸나 / 어디서 실패했나"를 쫓으려면 흩어지고 섞인 로그를 사람이 눈으로 맞춰야 한다. <br>

# 도입 — Micrometer Tracing (Brave)
요청마다 traceId를 만들고, 서비스 경계를 넘을 때 그걸 실어 보내게 했다. 두 가지가 필요했다. <br>
1. trace를 만들고 로그에 찍기
2. HTTP·Kafka로 넘길 때 헤더에 실어 보내기

## 1. 트레이싱 브릿지
`micrometer-tracing-bridge-brave`를 common-core(5개 서비스에 전파)와 gateway에 넣었다. <br>
이걸 넣으면 Spring Boot가 요청마다 span을 만들고 traceId/spanId를 MDC에 넣는다. 로그 패턴은 브릿지가 감지되면 자동으로 `[traceId-spanId]`를 붙인다 — 로그 설정은 따로 건드리지 않았다. <br>

## 2. HTTP 전파 — RestClient.Builder 주입
여기에 함정이 하나 있었다. `FundsClient`는 `RestClient.builder()`로 클라이언트를 직접 만들고 있었는데, **직접 만든 RestClient에는 트레이싱 계측이 붙지 않는다.** trace 헤더를 안 실어 보내니 funds가 새 trace를 시작해 버린다. 추적이 끊긴다. <br>
Spring Boot가 auto-config로 만들어 둔 `RestClient.Builder`(계측이 붙어 있다)를 주입받아 쓰도록 바꿨다. 그러면 요청을 보낼 때 trace 컨텍스트가 HTTP 헤더(B3)로 나가고 funds가 이어받는다. 회로차단기의 타임아웃 설정은 그대로 유지했다. <br>

## 3. Kafka 전파 — observation
Kafka는 기본적으로 trace 헤더를 넣지도 읽지도 않는다. producer·consumer 양쪽에 관찰(observation)을 켰다. <br>
```yaml
# trading (producer)
spring.kafka.template.observation-enabled: true
# projection (consumer)
spring.kafka.listener.observation-enabled: true
```
producer가 send할 때 trace를 Kafka 레코드 헤더에 쓰고, consumer가 그걸 읽어 이어받는다. 비동기 Kafka 홉을 넘어서도 trace가 살아있다. <br>

# 검증 — 한 traceId로 서비스를 넘어 추적
주문 한 건을 넣고 그 traceId로 로그를 훑었다. 매도 주문 하나가 매칭돼 정산·거래기록까지 도는 흐름이다. <br>

`grep <traceId>` 결과: <br>
```
[trading]    매도 주문 163 등록 → 매칭 1건 체결 → Kafka append
[funds]      코인 차감(매도 잠금) → 체결 정산(buyer 코인+, seller KRW+)   ← HTTP 전파
[projection] 거래 생성 tradeId=1                                        ← Kafka 전파
```
같은 traceId가 trading·funds·projection 셋에 걸쳐 찍혔다. **grep 한 번으로 이 주문이 서비스들을 어떻게 탔는지 쭉 볼 수 있다.** <br>
- trading↔funds가 같은 trace — HTTP 전파 확인
- trading→projection이 같은 trace — Kafka 전파 확인

(gateway는 요청마다 로그를 남기지 않아 grep엔 안 잡히지만, traceId를 만들어 전파하는 시작점이다.) <br>

# 시각화 — Zipkin 폭포수
여기까지가 **로그 상관관계**다. traceId로 로그를 이어 "어디서 실패했나"를 쫓을 수 있다. <br>
한 걸음 더 가서, 각 구간의 **소요 시간(span)**을 Zipkin으로 모아 폭포수로 봤다. 리포터(`zipkin-reporter-brave`)를 붙이고 Zipkin 컨테이너를 띄운 뒤, 각 서비스가 span을 그쪽으로 보내게 했다. <br>

매도 주문 한 건의 trace를 폭포수로 펼치면 이렇게 나온다. <br>
```
gateway    http post                          109ms   ← 사용자가 기다린 시간(루트)
 trading    POST /api/orders/sell             101ms
  trading    → funds debitCoin                 22ms
   funds       /internal/funds/coin/debit      17ms
  trading    order.log send (Kafka append)      3.7ms
  trading    → funds settle                    45ms
   funds       /internal/funds/settle          40ms
  trading    order.log send (Kafka append)     15ms
 projection order.log receive   (약 16초 뒤, 같은 trace)   ← Kafka 비동기 소비
 projection notification send (RabbitMQ)
```
두 가지가 눈에 보인다. <br>
1. **동기 hot path는 funds RPC가 지배한다.** 루트 109ms 중 funds 왕복 두 번(17+40)이 절반 이상이고, Kafka append는 3.7·15ms로 싸다. 이전에 서비스마다 타이머를 박아 짜맞추던 걸, 한 폭포수가 서비스 넘어 자동으로 보여준다.
2. **동기/비동기 경계가 한 trace에 다 담긴다.** DB projection과 알림은 사용자가 기다린 109ms에 없다. Kafka로 약 16초 뒤 소비돼 같은 traceId로 이어 붙는다 — hot path에서 빼낸(7~8단계) 그 부분이 trace에서도 떨어져 보인다.

다만 절대 수치는 로컬 환경 천장 안이라 그대로 믿긴 어렵고(같은 주문이 따뜻할 땐 더 빠르게도 나온다), **funds RPC가 동기 시간을 지배한다는 상대 구조**가 요점이다. <br>

## 노이즈 정리
처음 Zipkin을 켜니 5초마다 도는 헬스체크(`/actuator/health`)와 보안 필터 span이 화면을 도배했다. <br>
- `/actuator/**`는 `ObservationPredicate`로 트레이싱에서 제외했다.
- Spring Security 필터 span(`security filterchain` 등, 0.5ms 필터 내부)은 `management.observations.enable.spring.security: false`로 껐다. 안 그러면 헬스체크의 부모 span이 걸러진 뒤 이 보안 span들이 고아(orphan) trace로 떠 목록을 채운다.

이 둘을 걷어내니 목록에 실제 주문 trace만 남았다. <br>

# 정리
> 서비스를 나누자 요청 하나의 로그가 여러 서비스로 흩어져, 동시 요청 속에서 추적이 불가능해졌다. <br>
> 요청마다 traceId를 붙이고(브릿지) HTTP·Kafka로 전파하자, grep 한 번으로 한 요청이 gateway→trading→funds→projection을 어떻게 탔는지 이어 볼 수 있게 됐다. Zipkin으로 폭포수까지 보니, 동기 hot path는 funds RPC가 지배하고 DB projection·알림은 비동기로 떨어져 있는 게 한눈에 드러났다. <br>
> 직접 만든 RestClient는 계측이 붙지 않아 전파가 끊긴다 — auto-config 빌더를 주입받아야 한다. 이런 배선이 "라이브러리를 넣는 것"과 "실제로 전파되는 것" 사이의 차이였다. <br>
