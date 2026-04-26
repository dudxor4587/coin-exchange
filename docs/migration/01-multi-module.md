# 1단계 — 멀티모듈 분리

## 배경

직전까지 이 프로젝트는 **이벤트 기반 모듈러 모놀리스** 였다.
도메인별로 패키지가 잘 나뉘어 있고, RabbitMQ 이벤트로 도메인 간 통신을 하지만, 결국 단일 jar / 단일 프로세스 / 단일 DB 안에서 돌아간다.

여기서 다음 단계는 **진짜 분산 MSA로 가는 것**이고, 본 문서가 정리하는 1단계는 그 첫 걸음이다.

## 왜 굳이 멀티모듈부터 시작했는가

처음엔 "도메인별 패키지가 이미 잘 나뉘어 있는데, 멀티모듈을 굳이 거쳐야 하나? 바로 프로세스 분리로 가도 되지 않나?"라는 생각이 있었다.

하지만 패키지로만 나뉜 도메인은 다른 도메인 내부의 클래스를 자유롭게 `import`할 수 있다.
즉, 결합이 일어나도 빌드가 통과되기 때문에 결합 자체가 보이지 않는다.

만약 이 상태에서 바로 프로세스를 분리하면 어떻게 될까?
- 도메인을 떼어내려고 하니 컴파일이 깨진다 → 알고 보니 wallet이 order의 entity를 직접 참조하고 있었다
- 동시에 네트워크 호출 셋업, 배포 환경, 메시지 직렬화 같은 새 이슈도 같이 터진다
- 결합 문제와 분산 환경 문제가 섞여서 디버깅이 폭발한다

그래서 **단일 프로세스를 유지한 채 Gradle 모듈로만 먼저 자르는** 단계를 거치기로 했다.
모듈로 자르는 순간 의존성 방향이 빌드 시점에 강제되어, *진짜 분리 가능한 구조인지*를 컴파일러가 알려준다.
프로세스 분리의 리스크를 한 단계 앞에서, 비용 낮은 환경에서 흡수하는 것이 이 단계의 목적이다.

## 무엇을 했는가

단일 모듈이었던 프로젝트를 11개 모듈로 분리했다.

```
coin-exchange/
├── events-contract/        ← POJO 이벤트 (Spring/JPA 의존 0)
├── common-core/            ← BaseTimeEntity, BaseException, JwtTokenProvider, RabbitMQ 채널 상수
├── infra-notification/     ← 알림 서비스 + RabbitMQ listeners
├── domain-{coin,user,deposit,withdraw,trade,order,wallet}/
└── app/                    ← @SpringBootApplication, @Configuration, 부트스트랩
```

의존 방향은 `events-contract` / `common-core` 가 가장 leaf, 그 위에 `infra-notification`, 그 위에 도메인 모듈, 마지막으로 `app` 이 모든 것을 모아 실행하는 구조다.

모듈을 자르면서 드러난 결합은 분리 직전에 정리했다.
- `admin` 모듈을 폐기하고 각 도메인의 `admin/` 하위 패키지로 흡수했다. cross-domain 오케스트레이션이 없는, 권한 가드된 도메인 CRUD에 가까웠기 때문이다.
- `JwtTokenProvider`가 user 도메인의 `Role` enum을 import하던 결합을 String claim으로 바꿔 끊었다.
- `RabbitMQConfig`에 있던 채널 상수 51개를 `RabbitMQChannels`로 추출해 common-core로 옮겼다. 36개 파일이 의존하던 cross-cutting 상수였다.
- cross-domain 이벤트 17개를 `events-contract`로 이동시키고, 도메인 내부에서만 쓰는 이벤트는 그대로 두었다.

## events-contract의 의미

분리 작업에서 가장 신중했던 부분은 이벤트 클래스의 위치였다.
도메인 모듈끼리 직접 이벤트 클래스를 import하면 모듈 간 결합이 그대로 남고, 결국 처음 의도한 *컴파일러가 결합을 강제하는* 효과가 흐려지기 때문이다.

`events-contract`는 그래서 **MSA 단계에서의 wire contract 역할**을 미리 모듈화한 것이다.
지금은 같은 jar 안에서 Gradle project 의존(`implementation project(':events-contract')`)으로 묶이지만, 프로세스 분리가 끝나면 이 모듈이 외부에 publish되는 라이브러리(또는 Avro/Protobuf 스키마)로 발전할 자리다.

이 모듈이 Spring/JPA 의존을 0으로 유지한 이유도 같다. 다른 언어/스키마 시스템으로 교체될 가능성을 닫지 않기 위해서다.
