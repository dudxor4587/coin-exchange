직전까지 이 프로젝트는 이벤트 기반 모듈러 모놀리스였다. <br>
도메인별로 패키지가 잘 나뉘어 있고, RabbitMQ 이벤트로 도메인 간 통신을 하지만, 결국 단일 jar 하나로 단일 프로세스 안에서 돌아간다. <br>
여기서 다음 단계는 진짜 분산 MSA로 가는 것인데, 그 과정에서 멀티 모듈로 먼저 자르는 단계를 거쳤다. <br>

# 왜 멀티모듈부터 시작했는가
처음에는 "도메인별 패키지가 이미 잘 나뉘어 있는데, 멀티모듈을 굳이 거쳐야 하나? 바로 프로세스 분리로 가도 되지 않나?"라는 생각이 있었다. <br>
하지만 패키지로만 나뉜 도메인은 다른 도메인 내부의 클래스를 자유롭게 `import`할 수 있다. <br>
즉, 결합이 일어나도 빌드가 통과되기 때문에 결합 자체가 보이지 않는다. <br>

만약 이 상태에서 바로 프로세스를 분리하면 어떻게 될까. <br>
1. 도메인을 떼어내려고 하니 컴파일이 깨진다 → 알고 보니 wallet이 order의 엔티티를 직접 참조하고 있었다.
2. 동시에 네트워크 호출 셋업, 배포 환경, 메시지 직렬화 같은 새 이슈도 같이 터진다.
3. 결합 문제와 분산 환경 문제가 섞여서 디버깅이 폭발한다.

그래서 단일 프로세스를 유지한 채 Gradle 모듈로만 먼저 자르는 단계를 거치기로 했다. <br>
모듈로 자르는 순간 의존성 방향이 빌드 시점에 강제되어, 진짜 분리 가능한 구조인지 컴파일러가 알려준다. <br>
프로세스 분리의 위험을 한 단계 앞에서, 비용 낮은 환경에서 흡수하는 것이 이 단계의 목적이다. <br>

# 모듈 구조
단일 모듈이었던 프로젝트를 11개 모듈로 분리했다. <br>
```
coin-exchange/
├── events-contract/        ← POJO 이벤트 (Spring/JPA 의존 0)
├── common-core/            ← BaseTimeEntity, BaseException, JwtTokenProvider, RabbitMQ 채널 상수
├── infra-notification/     ← 알림 서비스 + RabbitMQ listeners
├── domain-{coin,user,deposit,withdraw,trade,order,wallet}/
└── app/                    ← @SpringBootApplication, @Configuration, 부트스트랩
```

의존 방향은 `events-contract`와 `common-core`가 가장 아래, 그 위에 `infra-notification`, 그 위에 도메인 모듈, 마지막으로 `app`이 모든 것을 모아 실행하는 구조다. <br>

# 모듈을 자르면서 드러난 결합
모듈로 자르고 나니 단일 모듈 시절엔 보이지 않던 결합이 컴파일 에러로 튀어나왔다. <br>
분리 직전에 다음과 같이 정리했다.

1. `admin` 모듈을 폐기하고 각 도메인의 `admin/` 하위 패키지로 흡수했다. 여러 도메인을 가로지르는 흐름이 없는, 권한 가드된 도메인 CRUD에 가까웠기 때문이다.
2. `JwtTokenProvider`가 user 도메인의 `Role` enum을 import하던 결합을 String claim으로 바꿔 끊었다.
3. `RabbitMQConfig`에 있던 채널 상수 51개를 `RabbitMQChannels`로 추출해 common-core로 옮겼다. 36개 파일이 의존하던 공통 상수였다.
4. 여러 도메인을 가로지르는 이벤트 17개를 `events-contract`로 옮기고, 도메인 내부에서만 쓰는 이벤트는 그대로 두었다.

# events-contract의 의미
분리 작업에서 가장 신중했던 부분은 이벤트 클래스의 위치였다. <br>
도메인 모듈끼리 직접 이벤트 클래스를 import하면 모듈 간 결합이 그대로 남고, 결국 처음 의도한 *컴파일러가 결합을 강제하는* 효과가 흐려지기 때문이다. <br>

`events-contract`는 그래서 **MSA 단계에서의 이벤트 계약 역할**을 미리 모듈화한 것이다. <br>
지금은 같은 jar 안에서 Gradle 의존(`implementation project(':events-contract')`)으로 묶이지만, 프로세스 분리가 끝나면 이 모듈이 외부에 publish되는 라이브러리 (또는 Avro/Protobuf 스키마) 로 발전할 자리다. <br>

이 모듈이 Spring/JPA 의존을 0으로 유지한 이유도 같다. <br>
다른 언어나 스키마 시스템으로 교체될 가능성을 닫지 않기 위해서다. <br>

# 후속 정비 — common-auth 분리
5단계 마무리 시점에 한 가지 더 손을 댔다. <br>
`common-core` 가 *security + jwt + web + jpa* 를 모두 `api` 의존으로 끌어당기는 구조였는데, 이게 가벼운 서비스 (notification) 에 자동설정 충돌을 만들었다. <br>
2단계 회고에서도 메모만 남기고 미뤘던 자리였는데, 5단계에 컨슈머 멱등성을 도입하면서 같은 자리에 한 번 더 부딪혀 정리하기로 했다. <br>

`common-core/.../auth/*` 10개 파일을 새 모듈 `common-auth/` 로 떼어내고, common-auth 에 *security + jwt + web* 의존을 두었다. <br>
common-core 는 *jpa + web* 만 남겼다. <br>
auth 가 필요한 서비스 (user, funds, trading) 가 build.gradle 에 명시적으로 common-auth 를 추가하고, notification 은 추가하지 않는다. <br>

효과는 단순했다. <br>
notification 이 security 라이브러리 자체를 안 받게 되어, `NotificationServiceApplication` 의 *SecurityAutoConfiguration 등 4개 exclude 어노테이션* 이 사라졌다. <br>
이 회피 코드는 *증상을 막던 것* 이지 *원인을 푼 것* 이 아니었는데, common-auth 분리로 원인 자체가 없어졌다. <br>

이건 1단계의 본질과 같은 작업이다. <br>
*의존이 컴파일 시점에 강제* 되어 *진짜 분리 가능한 구조인지* 가 build.gradle 단계에서 드러나도록. <br>

# 결론
> 단일 프로세스를 유지한 채 모듈만 먼저 자른 이유는, 결합 문제와 분산 환경 문제를 같이 터트리지 않기 위해서였다. <br>
> 모듈로 자르는 순간 의존 방향이 컴파일러에 강제되어, *진짜로 분리 가능한 구조인지* 가 빌드 단계에서 드러난다. <br>
> 다음 챕터에서 진짜 프로세스 분리에 들어갈 텐데, 그때 발견될 문제가 *이 챕터에서 먼저 드러난 문제* 만큼 단순하면 1단계의 가치가 증명되는 셈이다.
