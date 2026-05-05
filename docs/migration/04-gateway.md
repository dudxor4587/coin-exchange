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

## API 경로 정리

이 챕터를 거치며 그동안 어긋나 있던 API 경로 컨벤션도 손봤다.

```
변경 전:                              변경 후:
POST /api/user/login                 POST /api/users/login
POST /api/user/sign-up               POST /api/users/sign-up
POST /api/user/logout                POST /api/users/logout
GET  /api/users/by-email/{email}     GET  /internal/users/by-email/{email}
```

RESTful 컨벤션상 리소스 경로는 *복수형*이 표준인데, 그동안 단수/복수가 뒤섞여 있었다.
`by-email` 조회는 *funds-service 시더에서 사용자 ID를 받아오는 내부 전용*이라 외부에 노출하면 enumeration 공격 표면이 생긴다. `/internal/**` prefix로 분리해서 *gateway 라우팅에서 제외* — 외부 진입점에선 라우팅 자체가 없으니 404로 떨어지고, docker 내부망에서 funds-service가 user-service를 직접 부를 때만 동작.

`/internal/**` 같은 prefix는 업계 표준 컨벤션은 아니다. 큰 회사들은 보통 NetworkPolicy나 service mesh의 mTLS로 path와 무관하게 차단한다. 다만 우리 셋업처럼 *gateway 라우팅 규칙으로 외부 진입을 제어*하는 단계에서는 path-based 분리가 가장 *명시적이고 리뷰 가능*해 작은 팀에 잘 맞는다.

## 인증 정상 도입

원래 이 챕터에서는 *gateway 형태만* 갖추고 인증 강화는 뒤로 미루려 했다.
하지만 정리 도중 *지금이 정상 도입할 시점*이라는 결론이 났다 — 그동안 K6 부하 측정에서 인증을 우회한 건 토큰 발급 셋업이 귀찮아서였고, 그 임시 조치를 더 끌면 *분리의 형식*이 갖춰졌는데 *진입점에서의 검증*은 모래 위에 지은 셈이 된다.

진짜 표준 패턴으로 갔다.

```
[클라이언트]
    ↓ JWT cookie
[gateway: JWT 검증, X-User-Id/X-User-Role 헤더 주입]
    ↓ (헤더만)
[downstream: 헤더 신뢰, JWT 검증 안 함]
```

작업 단위:
- `TestSecurityConfig` 제거 — *모든 환경*에서 production SecurityConfig 사용
- `UserSeeder`가 비밀번호를 BCrypt로 인코딩 (NoOp이 사라졌으니까)
- `HeaderAuthenticationFilter` 신규 — `X-User-Id`/`X-User-Role` 헤더 읽어 SecurityContext 설정. *downstream에서 JWT 검증 안 함*
- `JwtAuthenticationFilter` 제거 — cookie의 JWT를 downstream에서 검증하던 코드는 dead code가 됨
- gateway의 `JwtAuthenticationFilter` (Spring Cloud Gateway의 `GlobalFilter`) 신규 — JWT 검증 후 헤더 주입. login/sign-up은 bypass
- docker-compose에서 user/funds/trading의 호스트 포트 노출 제거 (`ports` → `expose`로) — *외부에서 8082/8083/8084로 직접 호출 불가*
- K6 시나리오에 `setup()` 추가 — buyer/seller 로그인 후 cookie 획득, 본 시나리오에서 cookie 첨부

trust boundary가 의미를 가진다 — gateway가 외부 진입점이고, downstream은 *내부망에 있다는 가정으로 헤더를 신뢰*. 이 가정이 깨지지 않도록 호스트 포트도 같이 막았다 (외부에서 docker 내부망으로 직접 들어올 길이 없음).

## 서비스간 인증은 미룸

`funds-service → user-service`의 내부 API 호출(`/internal/users/by-email/...`)에는 인증이 없다. *내부 네트워크 신뢰 가정*에 기대고 있다.

이건 zero-trust 아키텍처가 아닐 때 흔한 선택이고, 작은 팀/조직에선 충분히 합리적. 만약 service-to-service에도 인증이 필요해지면 service token이나 mTLS를 별도 도입하면 된다 (운영 단계 이슈로 6~7단계와 같이 정리할 자리).

## 검증

K6 시나리오: **36,468 요청 / 0 에러, p95 494ms** (인증 도입 후 재측정).
인증 흐름을 K6 setup에서 정상적으로 거쳐도 응답 시간은 이전과 비슷 — 토큰 검증 비용이 무시할 만한 수준.

부수 검증:
- 토큰 없이 `/api/orders/buy` 호출 → **HTTP 401** (gateway에서 차단)
- `localhost:8082/8083/8084`로 직접 호출 → **Connection Refused** (호스트 포트 미노출)
- 정상 토큰으로 호출 → **HTTP 200**

gateway가 진짜 trust boundary로 동작한다는 게 확인됐다.

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
