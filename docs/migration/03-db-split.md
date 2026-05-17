2단계가 끝난 직후 시스템은 **5개 서비스가 각자 다른 프로세스에서 도는데, MySQL 인스턴스 하나를 공유**하는 모양이었다. <br>
이 상태는 진짜 분산 시스템이라기보다 분산 흉내에 가까웠다. <br>

1. 어떤 도메인이 DDL 변경을 하면 다른 도메인이 그 영향을 받음.
2. 한 도메인의 무거운 쿼리가 다른 도메인의 커넥션 풀까지 잡아먹음.
3. 결과적으로 직전 챕터의 가장 끈질긴 병목이었던 HikariCP 공유 문제가 그대로 남아 있음.

3단계는 그걸 풀어내는 단계다. <br>
**자원까지 떼어내야 진짜 격리가 완성된다**. <br>

# 무엇을 했는가
MySQL 컨테이너를 4개로 늘리고, 각 서비스가 자기 전용 인스턴스 + 자기 전용 schema를 쓰도록 바꿨다. <br>

```
[user-service]    → coin-exchange-user-db    (user_db)     :3303
[funds-service]   → coin-exchange-funds-db   (funds_db)    :3304
[trading-service] → coin-exchange-trading-db (trading_db)  :3305
[메인 app]        → coin-exchange-main-db    (main_db)     :3302
[notification]    → DB 사용 안 함
```

각 DB의 테이블도 도메인별로 깔끔히 떨어졌다.
- `user_db`: user
- `funds_db`: wallet, coin_wallet, deposit, withdraw
- `trading_db`: order, order_book, trade
- `main_db`: coin

# 한 번에 schema + instance 분리한 이유
처음엔 *스키마만 분리하고 인스턴스는 공유* 하는 점진적 경로를 고려했다. <br>
하지만 schema만 분리하면 자원 격리는 안 된다 — 같은 MySQL 프로세스가 여전히 모든 schema를 다루니까. <br>
분리의 진짜 가치인 자원 격리가 안 생긴다. <br>

그래서 한 번에 둘 다 갔다. <br>
2단계까지의 정리 덕분에 서로 다른 서비스가 같은 DB 테이블에 접근하는 코드가 0이라는 게 확인되어 있었기 때문에, 추가 코드 변경 없이 인스턴스만 늘리고 datasource URL만 갈아끼우면 끝나는 작업이었다. <br>

# 어려웠던 부분
거의 없었다. <br>
2단계에서 이미 *대부분의 결합을 RabbitMQ 이벤트와 REST API로 풀어낸 상태* 였기 때문이다. <br>

만약 2단계에서 코드 결합을 풀지 않고 곧장 DB 분리를 시도했다면, "다른 도메인 테이블을 직접 join하던 쿼리가 깨진다" 같은 식의 문제가 폭발했을 것이다. <br>
*분리는 결합을 끊고 나서 자원을 떼어내는 것이 한참 안전하다* 는 게 이번에 다시 확인됐다. <br>

다만 한 가지 — 시더의 서비스 간 의존은 그대로 남아 있었다. <br>
funds-service의 시더가 user-service를 REST API로 호출해 buyer/seller ID를 가져오는 구조였는데, 이건 2단계에서 만들어 둔 것이라 DB 분리 후에도 *그대로 동작* 했다. <br>
"DB 분리 직전에 API를 만들어 두는 것이 직후에 만드는 것보다 훨씬 싸다" 는 2단계의 작은 교훈이 여기서 직접적인 이득으로 돌아왔다. <br>

# 검증
K6 시나리오 회귀: **35,916 요청 / 0 에러, p95 557ms**. <br>
이전 단계(단일 MySQL 공유) 대비 응답 시간이 약간 빨라졌다 — 도메인별 HikariCP 풀이 격리되어 *서로의 커넥션을 빼앗지 않는다* 는 게 추정 원인이다. <br>

각 DB 컨테이너에 들어가서 `SHOW TABLES`를 찍어 보면 자기 도메인 테이블만 있다. <br>
더 이상 한 schema에 모든 테이블이 모여 있지 않다. <br>

# 결론
> 현재 시스템 모양: <br>
> ```
> [trading-service]  (8084) ─── trading-db  (3305)
> [funds-service]    (8083) ─── funds-db    (3304)
> [user-service]     (8082) ─── user-db     (3303)
> [notification]     (8081) ─── (DB 없음)
> [메인 app]         (8080) ─── main-db     (3302)
>
>       모두 같은 RabbitMQ + Redis(trading만) 공유
> ```
> 도메인별로 *코드*도 *프로세스*도 *데이터*도 떨어져 있는 상태가 됐다. <br>
> 공유되는 것은 메시징 인프라(RabbitMQ)와 캐시(Redis)뿐이며, 이것들은 의도적으로 공유하는 공유 인프라 계층이다. <br>
> 다음 단계는 4단계 — 인증 처리. 지금은 user-service가 발행한 JWT를 다른 서비스가 같은 secret으로 검증하지만, 진짜 서비스 간 인증/권한은 더 정리가 필요하다 (gateway 도입 검토 등).
