# 4단계 — API Gateway 도입 + 메인 app 정리

## 배경

3단계까지 끝나고 나니 시스템은 *코드도 프로세스도 데이터도 분리*된 상태였다. 그러나 외부에서 보면 여전히 어색한 구석이 있었다.

- 클라이언트가 `8082`(user), `8083`(funds), `8084`(trading)을 직접 알아야 호출 가능
- 단일 진입점이 없으니 라우팅/CORS/rate limit 같은 cross-cutting을 모든 서비스가 각자 처리
- *메인 app(8080)*은 사실상 빈 컨테이너가 된 채로 남아 있었다 — coin 시드 박는 일만 하고 끝

이번 단계는 그 둘을 같이 처리했다. **API Gateway를 도입해 단일 진입점을 만들고, 동시에 빈 컨테이너가 된 메인 app을 정리**했다.

## Spring Cloud Gateway

새 모듈 `gateway/`를 만들고 Spring Cloud Gateway 의존을 넣었다. 라우팅 규칙은 application.yml에 선언적으로:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://coin-exchange-user:8082
          predicates:
            - Path=/api/user/**,/api/users/**
        - id: trading-service
          uri: http://coin-exchange-trading:8084
          predicates:
            - Path=/api/orders/**,/api/trades/**
        - id: funds-service
          uri: http://coin-exchange-funds:8083
          predicates:
            - Path=/api/wallets/**,/api/deposits/**,/api/withdraws/**,/admin/**
```

gateway는 8080을 차지하고, 클라이언트는 이제 `localhost:8080`만 알면 된다. Path prefix를 보고 알맞은 downstream으로 forward.

K6의 `BASE_URL`을 `8080`으로 되돌렸다 — *직전 단계에서 trading-service에 직접 꽂던 것*을 다시 단일 진입점으로 옮긴 것.

## 메인 app 제거 — 빈 컨테이너의 마무리

각 서비스 분리가 끝나고 나서 메인 app에 남아 있던 건 `domain-coin` 하나뿐이었다.
coin 도메인은:

- `Coin` 엔티티 + `CoinRepository` (BTC, ETH 두 row)
- `CoinController` (빈 클래스)
- `CoinService.findBySymbol` (어디서도 호출 안 됨)

런타임 기능이 거의 없는 master data 테이블.
이 상태에서 메인 app을 별도 컨테이너로 띄우는 건 *분리의 형식만 갖추기 위한 운영 부담*이었다 — *"분리는 기능적 가치가 있을 때만"* 의 원칙(2단계 회고 참고)이 다시 한 번 적용되어야 했다.

처리 방향은 두 가지였다:
1. coin 도메인을 어딘가로 흡수 (메인 app 삭제)
2. 메인 app을 그대로 두고 coin 시드 컨테이너로 유지

코인 ID를 가장 자주 쓰는 도메인이 `trading`이고, 가까운 미래에 코인 정보(가격 피드 등)가 추가된다면 그것도 trading과 가까운 위치가 자연스러우니, **coin 도메인을 trading-service로 흡수**하기로 했다.

작업 자체는 작았다:
- `app-trading/build.gradle`에 `domain-coin` 의존 추가
- `TradingServiceApplication`의 `@EntityScan`/`@EnableJpaRepositories`에 `com.coinexchange.coin.*` 추가
- 메인 app의 `LoadTestSeeder`에서 coin 시드 부분을 떼어내 `app-trading/seed/CoinSeeder.java`로 이동
- `app/` 모듈 + `Dockerfile` + docker-compose의 `coin-exchange-was`/`coin-exchange-main-db` 항목 삭제

`coin` 테이블이 `trading-db`로 들어가서, 이제 trading-db는 `coin / order / order_book / trade` 4개 테이블을 가진다.
DB 인스턴스도 4개에서 3개로 줄었다 (user-db / funds-db / trading-db).

## 분리의 형식 vs 기능적 가치

이 챕터를 거치면서 다시 확인한 점 — **분리는 1:1 모듈:서비스 매핑이 아니다**. 기능적 가치가 있는 곳까지만 분리하고, 가치가 없는 곳은 합친다.

- coin 도메인을 *임시로* 메인 app에 두었던 건 다른 분리 작업의 진행을 막지 않기 위한 일시 조치였다
- 4단계가 정리하기 자연스러운 시점이었던 이유: 운영 형태가 굳어지는 단계라 *빈 컨테이너의 잔존 가치*가 0에 수렴
- coin이 미래에 자체 도메인 가치를 갖게 되면 (admin 엔드포인트, 가격 피드, 신규 등록) 그땐 trading에서 다시 떼어내면 됨 — 모듈 구조는 그대로 유지되어 있으니까

## 인증 처리는 어떻게 했는가

이 챕터의 원래 계획에는 *서비스간 인증/권한 정리*도 포함되어 있었다.
하지만 현재 구조를 보면:

- 테스트 프로파일(K6 부하 측정 환경)은 `TestSecurityConfig.permitAll()`로 인증을 비활성화 — gateway에서 별도 검증 없이 forward해도 정상 동작
- 운영 프로파일은 각 서비스가 같은 JWT secret으로 자체 검증 (cookie의 accessToken)

즉 *현재 상태*에서 gateway는 라우팅만 하면 되고, 토큰 검증은 downstream 서비스가 알아서 한다. JWT cookie는 gateway가 그대로 forward하고, downstream의 `JwtAuthenticationFilter`가 검증.

이 구조의 한계는 분명하다:
- gateway에서 한 번 검증하면 downstream이 다시 검증할 필요 없는데, 지금은 *각 서비스가 매 요청마다 JWT를 다시 푼다*
- 서비스간 호출(funds → user의 lookup API)에는 인증이 없음 — 내부 네트워크 신뢰 가정

이걸 *제대로* 하려면 gateway에서 토큰 검증 후 user-id를 헤더로 전달, downstream은 헤더만 신뢰하는 구조가 맞고, 서비스간 호출은 service token이나 mTLS로 보호해야 한다.
다만 이건 *진짜 운영 환경 수준의 마무리*이고, 이번 단계에서는 **단일 진입점이라는 형태부터 갖추는 것**까지만 담았다. 인증 강화는 6~7단계의 운영 이슈와 함께 정리하는 게 맞다고 봤다.

## 검증

K6 시나리오: **32,652 요청 / 0 에러, p95 610ms**.
gateway 한 단계가 끼었지만 응답 시간 차이는 미미.

`/api/orders/buy` 요청이 `localhost:8080` → gateway → trading-service(8084) 순으로 라우팅되어 정상 처리. trading-db에 coin/order/trade가 같이 들어가 있고, main-db 컨테이너는 더 이상 존재하지 않음.

## 마무리

현재 시스템 모양:

```
[클라이언트]
     ↓
[gateway]  (8080) ─── 라우팅만
     ↓
 ├── [user-service]    (8082) ─── user-db
 ├── [funds-service]   (8083) ─── funds-db
 └── [trading-service] (8084) ─── trading-db (+ coin)

         [notification-service] (8081) ←── RabbitMQ
```

도메인 4개(user / funds / trading / notification) + gateway 1개 = 진짜 *MSA의 형태*가 갖춰졌다.
물리적으로도 논리적으로도 분리가 일관성 있게 적용되어 있고, 단일 진입점을 통해 외부에 노출된다.

다음 단계는 5단계 — **Kafka 마이그레이션**. RabbitMQ를 Kafka로 갈아 끼우면서 *파티션 키 = coinId*로 매칭엔진 순서 보장 + 코인별 병렬 처리를 손에 쥐는 단계다. 매칭엔진 단독 측정이 의미를 가지기 시작하는 시점이기도 하다.
