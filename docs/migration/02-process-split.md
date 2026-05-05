# 2단계 — 프로세스 분리

## 배경

1단계에서 도메인을 11개 Gradle 모듈로 잘라 놓은 직후의 상태는, *컴파일러가 결합을 잡아 주는 모듈러 모놀리스*다.
경계는 그어졌지만 여전히 단일 jar / 단일 프로세스 / 단일 DB.
이 단계의 목표는 **그 모듈 경계를 진짜 프로세스 경계로 끌어올리는 것** — 도메인을 별도 Spring Boot 앱과 별도 컨테이너로 떼어내고, 통신을 네트워크(REST + RabbitMQ)로 옮긴다.

## 어떻게 묶을지 — 도메인 모듈 1:1 분리에서 4 services로

처음 계획은 7개 도메인 모듈을 그대로 7개 서비스로 분리하는 것이었다.
하지만 실제로 한 도메인을 떼어내 보고 나니, 1:1 매핑이 항상 옳지는 않았다.

- `coin` 도메인은 빈 컨트롤러 + 호출되지 않는 서비스 + 마스터데이터 테이블 하나뿐이다. 분리해도 런타임에 할 일이 없다 — *기능적 가치 없는 분리는 운영 부담만 늘린다.*
- `order`와 `trade`는 매칭엔진 hot path와 Redis OrderBook을 공유한다. 분리하면 OrderBook이 cross-process 공유 상태가 되어 락/일관성 문제가 생긴다.
- `wallet`/`deposit`/`withdraw`는 셋 다 잔고 도메인이고 3단계 DB 분리 시 같은 잔고 테이블을 공유한다.

그래서 분리 단위를 *기능적 결합과 트랜잭션 경계 기준*으로 다시 잡았다.

| 서비스 | 도메인 |
|---|---|
| notification-service | infra-notification |
| user-service | user |
| funds-service | wallet + deposit + withdraw |
| trading-service | order + trade |
| (메인 app) | coin + 미분리 cross-cutting |

## 사전 정리 — 도메인 → infra-notification 직접 호출 끊기

분리에 들어가기 전에 결합부터 끊어야 했다.
`wallet`, `order`, `withdraw` 세 도메인이 `infra-notification`의 `NotificationService`를 직접 import해서 동기 호출하고 있었다 (12개 호출 지점).
이대로 분리하면 도메인 서비스가 알림 서비스의 jar를 그대로 끼고 있어야 한다 — 의미가 없다.

`NotificationRequestedEvent(userId, message)` 한 종류를 만들어 events-contract에 두고, 12개 호출을 모두 이벤트 발행으로 바꿨다.
도메인 쪽은 메시지 포맷팅까지만 책임지고 RabbitMQ로 던진다. infra-notification은 그걸 받아서 sender로 보낸다.
분리 시점에 결합이 *이미* 끊겨 있도록 한 단계 앞에서 처리한 셈이다.

## 첫 분리 — notification-service

가장 결합 적은 listener-only 도메인부터 시작했다. 외부에서 호출하지 않고, 다른 도메인에서 push하는 이벤트만 소비한다.

- `infra-notification`에 `@SpringBootApplication` + main + 자체 `application.yml` 추가, 별도 Dockerfile/컨테이너 (8081)
- 자체 `RabbitMQConfig`에서 큐/익스체인지/바인딩 + Jackson 컨버터 선언
- 메인 app에서는 `infra-notification` Gradle 의존 제거 — 더 이상 같은 jar에 들어가지 않음

기동 직후 한 번 실패가 있었다. `common-core`가 `spring-security`를 `api` 의존으로 끼고 있어, notification-service에 `ManagementWebSecurityAutoConfiguration`이 따라 들어와서 충돌이 났다.
필요한 만큼만 exclude로 처리했지만 — common-core가 auth/web/jpa starter를 모두 묶어 두는 구조라서 가벼운 서비스에 과한 의존이 따라온다는 건 다음에 슬리밍 검토할 후보로 메모만 해 두었다.

검증은 K6 시나리오를 그대로 재실행했다. 메인 app의 알림 listener 호출 0회, notification-service에서 5,000건 정도 처리. 분리가 의도대로 동작.

## 두 번째 분리 — user-service

인증 도메인은 생각보다 단순하게 떼어졌다. 다른 도메인이 `User` 엔티티 자체를 import하지 않고 `userId`(Long)만 쓰고 있었기 때문이다.

다만 한 곳이 걸렸다 — 부하 테스트 시더.
지금까지는 메인 app의 `LoadTestSeeder`가 `User`를 직접 만들어서 `user.getId()`로 wallet seed에 연결했다.
분리하고 나니 메인 app은 `User` 엔티티를 더 이상 가지고 있지 않다.

처음에는 시더에 `SEED_BUYER_ID = 1L` 같은 상수를 박는 방식으로 가려 했다.
user-service가 먼저 시드해서 자동증가로 1, 2를 받을 거니까, 메인 app은 그걸 가정하고 wallet만 만든다 — 라는 식이다.

하지만 이건 *지금 단일 DB라는 임시 상태에 안주한 풀이*였다.
3단계에서 DB가 분리되면 메인 app은 `User` 테이블을 *물리적으로* 못 본다.
하드코딩 ID로는 더 이상 못 가는 시점이 어차피 온다.

그래서 user-service에 `GET /api/users/by-email/{email}` 엔드포인트를 만들고, 메인 app 시더가 `RestClient`로 ID를 조회하도록 바꿨다.
지금 만들어 두는 API가 다음 챕터의 비용을 줄인다 — *분리 단계에서 미리 service API를 만들어 두는 것이 DB 분리 직전에 몰아서 만드는 것보다 안전하다*는 작은 교훈.

## 세 번째 분리 — funds-service

여기서부터는 단일 도메인 분리가 아니라 **세 도메인을 한 서비스로 묶는 작업**이었다 — `wallet` + `deposit` + `withdraw`.
잔고를 만지는 도메인 셋이 같은 서비스로 가는 게 자연스러웠다. 한 트랜잭션으로 묶이지는 않지만 (이미 SAGA로 풀려 있다), 3단계 DB 분리 시 같은 잔고 테이블을 공유하기 때문이다.

세 도메인을 묶는 모듈을 새로 만들었다 — `app-funds/`. 메인 `app/`이 가지던 composition root 역할을 funds 영역에서 따로 들어주는 모듈이다. 안에 `FundsServiceApplication`, `application.yml`, `RabbitMQConfig`, 그리고 자체 `FundsSeeder`를 두었다.

### 이벤트 브릿지의 위치 문제

분리 직전 발견한 가장 큰 함정은 **Spring 이벤트 → RabbitMQ 브릿지 핸들러의 위치**였다.

`BuyOrderReadyEvent`는 `wallet` 도메인이 발행하는 이벤트지만, 그 브릿지(`@TransactionalEventListener`로 받아서 RabbitMQ로 forward하는 핸들러)가 `domain-order` 안에 있었다.
단일 프로세스에선 같은 ApplicationContext라서 어디 있어도 잡혔지만, 분리하면 *발행자 프로세스에 브릿지가 없으면 이벤트가 RabbitMQ로 안 흐른다*.

브릿지를 발행자가 있는 도메인으로 옮겼다 — `BuyOrderReadyEventHandler`/`SellOrderReadyEventHandler`를 `domain-order/infra/publisher`에서 `domain-wallet/infra`로.
원칙은 단순하다: **이벤트 브릿지는 발행자와 같은 모듈에 둔다.**

다른 한쪽 — `NotificationRequestedEvent`는 wallet/order/withdraw 셋 다 발행한다. 메인 app과 funds-service 양쪽에서 발행하니, 각 composition root에 자체 브릿지를 둘 수밖에 없었다 (`app/`과 `app-funds/`에 같은 핸들러가 한 개씩). 도메인 모듈에 두면 한 ApplicationContext 안에 두 개의 브릿지가 동시에 살아 이중 발행이 되어 버린다.

### JPA 스캔 범위

처음 기동에 `Found 0 JPA repository interfaces` 로 떨어졌다.
`@SpringBootApplication`은 자기 패키지(`com.coinexchange.funds`)를 기본 스캔 base로 잡는데, 엔티티와 레포지토리는 `com.coinexchange.wallet`, `com.coinexchange.deposit`, `com.coinexchange.withdraw`에 흩어져 있기 때문이다.

`@EntityScan`과 `@EnableJpaRepositories`에 명시적으로 패키지를 나열해 해결했다. user-service에선 SpringBootApplication 패키지와 도메인 패키지가 같아서 (`com.coinexchange.user.*`) 자동 스캔으로 풀렸지만, 묶음 서비스는 *composition root의 패키지가 도메인 패키지와 분리*되기 때문에 명시 설정이 필요하다.

### 시더 분리

메인 app의 `LoadTestSeeder`가 가지고 있던 wallet/coinwallet 시드는 funds-service의 자체 `FundsSeeder`로 옮겼다.
user-service에서 ID를 받아오는 로직(`/api/users/by-email/{email}` 호출)은 그대로 재사용 — 바로 직전 단계에서 만들어 둔 API가 두 번째 시더에서도 그대로 쓰였다.

`coinId`는 일단 `1L`로 하드코딩했다. 메인 app의 `LoadTestSeeder`가 BTC 코인을 id=1로 시드하는 것을 가정한다.
이건 *coin이 별도 서비스로 분리되지 않았기 때문에* 받아들인 임시 결합이다. coin 분리가 의미를 가질 시점이 오면 그때 풀면 된다.

### 검증

K6 시나리오 회귀: 33,819 요청 / 0 에러.
메인 was에서 지갑 처리 0회, funds-service에서 10,000회 이상 처리 — 잔고 도메인이 진짜로 다른 프로세스에서 굴러간다는 게 확인됐다.

## 네 번째 분리 — trading-service

마지막은 `order` + `trade` 묶음. 매칭엔진 hot path와 Redis OrderBook을 가지고 있어, 분리해도 의미가 있는 마지막 큰 덩어리였다.

`app-trading/`이라는 composition root 모듈을 새로 만들고 두 도메인을 묶었다. 매칭엔진이 Redis를 쓰기 때문에 자체 `RedisConfig`가 필요했고, 메인 app에서 Redis 빈을 선언하던 것을 그대로 이 쪽으로 옮겨왔다.

### 클래스패스 자원의 자리

기동 중에 `scripts/matchingLogic.lua` 파일을 못 찾는다고 떨어졌다.
이 파일은 매칭엔진이 Redis Lua 스크립트로 OrderBook을 다루는 데 쓰는데, 원래 `app/src/main/resources/scripts/`에 있었다 — *메인 app의 리소스 디렉토리*.

분리 전엔 메인 app이 모든 도메인을 번들링했기 때문에 그 위치도 무관히 잡혔지만, 분리 후 trading-service의 jar에 그 파일이 들어가지 않으니 클래스패스에 없다.
파일을 `domain-order/src/main/resources/scripts/`로 옮겼다 — *사용하는 도메인이 자기 자원을 가지고 다니도록*.

비슷한 원칙이 한 단계 앞 funds-service의 시더(자기 도메인 시드는 자기 서비스에서)에서도 있었다. **각 서비스가 필요한 자원/시드/설정을 자기 안에 가지고 있어야 분리가 진짜로 독립적이 된다**는 점.

### K6의 새 base URL

주문 엔드포인트가 메인 app에서 trading-service(8084)로 옮겨갔다. K6 시나리오의 `BASE_URL`을 8084로 바꿨다.
이 시점부터 메인 app(8080)은 부하 트래픽이 흐르지 않는 상태가 됐다 — 사실상 coin 시드만 들고 있는 빈 컨테이너에 가까워졌다.

### 검증

K6: 29,869 요청 / 0 에러.
서비스별 처리 분포:
- trading-service: 매칭 처리 5,586건 (Redis OrderBook 기준)
- funds-service: 잔고 처리 21,037건 (생성·체결·완료 이벤트 합)
- notification-service: 알림 15,192건
- 메인 app(was): 0건 (예상대로)

`매수 → 잔고 잠금 → 주문장 등록 → 매칭 → 체결 → 잔고 정산 → 알림` 의 풀 플로우가 *5개 서로 다른 프로세스*에 걸쳐 RabbitMQ로 흘러가서 끝까지 떨어진다는 것이 확인됐다.

## 마무리

2단계 끝나고 나니 시스템 모양이 이렇게 됐다.

```
[trading-service] (8084)         → 주문 + 매칭 + Redis OrderBook
[funds-service]   (8083)         → 잔고 (wallet/deposit/withdraw)
[user-service]    (8082)         → 인증 + 사용자 마스터데이터
[notification-service] (8081)    → 알림 컨슈머
[메인 app]        (8080)         → coin 마스터데이터 (사실상 비어 있음)
```

도메인을 1:1로 떼지 않고 *기능적 결합과 트랜잭션 경계 기준*으로 4 services로 묶은 결정이 결과적으로 무리 없이 굴러갔다.
한 번에 떼지 않고 (1) 결합부터 끊고 (2) 작은 서비스부터 큰 서비스 순으로 분리한 순서도, *분리 도중 발견된 문제 (브릿지 위치, JPA 스캔, 클래스패스 자원)*가 한꺼번에 폭발하지 않게 해 주었다.

다음 단계는 3단계 — DB schema/인스턴스 분리. 지금까지는 5개 서비스가 *물리적으로 같은 MySQL을 공유*하기 때문에 진짜 분산 시스템이 아니다. 다음 챕터에서 자원까지 떼어내야 풀 MSA에 가까워진다.
