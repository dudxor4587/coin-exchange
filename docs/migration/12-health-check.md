회로차단기·트레이싱에 이어, 서비스가 나뉜 환경에서 마지막으로 손볼 운영 이슈는 **헬스체크**다. <br>
k8s는 서비스한테 "너 지금 트래픽 받아도 돼?"를 계속 물어보고, 그 답으로 **재시작할지 / 트래픽 보낼지**를 정한다. 그런데 지금 그 답이 두 상황에서 틀렸다. <br>

# 문제 — 헬스체크가 잘못된 답을 한다
현재는 `/actuator/health` 하나로 "괜찮냐"를 묻는다. 두 케이스를 재현했다. <br>

## db를 정지 → "안 괜찮아"인데 실제론 괜찮다
trading-db를 멈추니 trading의 `/actuator/health`가 응답을 못 했다(db 지표가 죽은 커넥션을 검증하려다 멈춤). <br>
그런데 같은 상태에서 **주문은 200으로 정상 처리됐다.** trading은 hot path에서 db를 쓰지 않기 때문이다(7~9단계에서 주문 저장을 projection으로 빼냈다). <br>
즉 trading은 서빙이 멀쩡한데 health가 "안 괜찮아"라고 한다. k8s가 이 신호를 보면 멀쩡한 trading을 라우팅에서 빼거나 재시작한다 — 재시작해도 죽은 건 db라 소용없다. <br>

## kafka를 정지 → "괜찮아"인데 실제론 못 한다
반대로 kafka를 멈추니 health는 **200(UP)**을 유지했다. 그런데 **주문은 실패했다.** durable append를 못 하기 때문이다. <br>
Spring Boot는 db·redis·rabbit은 헬스 지표를 자동으로 만들어주지만 **kafka는 안 만들어준다.** 그래서 kafka가 죽어도 health가 눈치채지 못하고, k8s는 계속 트래픽을 보낸다. <br>

## 왜 이렇게 어긋났나
두 가지가 겹쳤다. <br>
1. **Spring 기본값이 옛 구조에 맞춰져 있었다.** trading에 DataSource가 있으면 Spring이 db 지표를 자동으로 붙인다. 예전엔 trading이 주문을 DB에 동기로 저장했으니 맞는 지표였는데, 구조를 바꿔 db를 안 쓰게 됐어도 헬스 설정은 안 따라왔다.
2. **liveness/readiness가 안 나뉘어 있었다.** health 하나가 "재시작해라"와 "트래픽 빼라"를 뭉뚱그렸다. (Spring은 이 분리를 k8s에서 돌 때만 자동으로 켠다. 우리는 compose로 개발해 꺼져 있었다.)

# 고침 1 — db는 구조로 뿌리뽑았다
db를 헬스에서 빼는 방법은 두 가지였다. (a) "db 검사 하지 마"로 설정에서 가리기, (b) trading이 db를 아예 안 갖게 하기. <br>
trading이 db를 붙들고 있는 유일한 이유는 **coin 시더**(BTC 마스터데이터 한 줄 심기)였다. 이건 구조적 찌꺼기였다 — 주문은 hot path에서 coin/db를 안 쓰는데, 시더 때문에 DataSource가 남아 있었다. <br>
그래서 **coin 시더를 projection으로 옮겼다.** projection이 이미 trading-db(Order/Trade)를 쓰는 주인이니 coin 시딩도 거기가 맞다. trading은 domain-coin·JPA·datasource를 통째로 떼고 **무상태(Redis + Kafka + funds RPC)**가 됐다. <br>
DataSource가 없어지자 Spring이 db 지표를 붙일 이유도 사라졌다 — 설정으로 가린 게 아니라 원인이 없어졌다. <br>

# 고침 2 — liveness/readiness 분리 + 진짜 의존성만
health를 두 질문으로 나눴다. <br>
- **liveness** (`/health/liveness`) — 앱 프로세스가 살아있나. 아니면 재시작. **의존성과 무관.**
- **readiness** (`/health/readiness`) — 트래픽 받아 일할 수 있나. 아니면 트래픽만 빼고, 재시작은 안 함.

readiness에는 **그 서비스가 서빙에 진짜 필요한 것만** 넣었다. <br>
- trading → redis(매칭) + kafka(로그). db는 안 쓰니 없고, 회로차단기는 open이어도 정상 동작이라 제외.
- projection → db + kafka + rabbit (다 필요).

kafka는 Spring이 기본 지표를 안 만들어주므로, AdminClient로 브로커 도달을 2초 안에 확인하는 **커스텀 지표**를 작성해 넣었다. <br>

# 검증 — 같은 장애, 다른 답
| 상황 | 이전 | 이후 |
|---|---|---|
| **trading-db 정지** | health 응답 못 함(먹통) → 재시작/라우팅 제외 대상 | **readiness UP, 주문 정상** — db와 무관 |
| **kafka 정지** | health UP → 트래픽 계속(주문 실패) | **readiness DOWN**(트래픽 제외) + **liveness UP**(재시작 X) |

db가 죽어도 trading은 트래픽을 받고, kafka가 죽으면 트래픽만 빠졌다가 kafka가 살아나면 스스로 돌아온다. <br>

# 정리
> 서비스가 나뉘자 헬스체크가 "서빙에 진짜 필요한 것"과 어긋났다. 안 쓰는 db 때문에 멀쩡한 trading을 죽은 것으로, 쓰는 kafka는 안 봐서 죽은 걸 산 것으로 답했다. <br>
> db는 설정으로 가리지 않고 구조로 없앴다 — coin 시더가 trading이 db를 붙든 유일한 이유였고, 그걸 projection으로 옮겨 trading을 무상태로 만들었다. kafka는 커스텀 지표로 채우고, liveness/readiness를 나눠 "재시작"과 "트래픽 빼기"를 구분했다. <br>
> 이것도 k8s 매니페스트가 RabbitMQ로 stale했던 것과 같은 결이다 — 구조가 바뀔 때 뒤처진 횡단 설정을 아키텍처에 맞춘 작업이다. <br>
