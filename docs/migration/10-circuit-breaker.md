시스템이 여러 서비스로 갈라지면서, 단일 프로세스일 땐 없던 문제가 생겼다. **한 서비스가 죽으면 그걸 부르는 쪽은 어떻게 되나?** <br>
trading은 주문마다 funds를 동기로 부른다(`debitKrw`, `settle`). 그럼 funds가 죽으면 trading은 어떻게 되는가. 이 챕터는 그걸 측정으로 확인하고 막는다. <br>

# 문제 — funds가 죽으면 trading이 같이 죽는다
우선 정말 그런지부터 재현했다. <br>
funds를 `docker pause`로 멈추고, 주문 250건을 동시에 던졌다(Tomcat 워커는 기본 200개). <br>

결과는 이랬다. <br>
- 완료된 주문 **0 / 250** — 전부 응답 없이 멈춤
- Tomcat 워커 **200개 전부 `WAITING (parking)`**
- 그 200개 전부 **`FundsClient.debitKrw`에서 대기** 중
- trading의 `/actuator/health`(funds와 무관한 엔드포인트)마저 **타임아웃**
- compose 헬스체크가 trading을 **`unhealthy`**로 판정

스레드 덤프의 스택이 원인을 그대로 보여줬다. <br>
```
OrderController.buyLimitOrder
  → OrderFlowService.placeBuyOrder
    → FundsClient.debitKrw     ← 여기서 park (funds 응답 대기)
      → LockSupport.park
```
funds가 응답을 안 하니, 주문을 처리하던 워커 스레드가 funds 응답을 기다리며 멈춘다. 주문이 쏟아지면 워커 200개가 전부 이렇게 묶이고, **그러면 funds와 상관없는 요청(health 포함)도 받을 워커가 없다.** funds 하나가 trading 전체를 멈춘 것이다. <br>

왜 "영원히" 기다렸는지 보니, `FundsClient`의 `RestClient`에 **타임아웃이 없었다.** 응답이 안 오면 무한정 기다린다. <br>

원인이 두 겹이었다. <br>
1. **타임아웃 없음** — funds가 응답 안 하면 워커가 무한 대기한다.
2. **차단 장치 없음** — funds가 죽어있어도 모든 요청이 계속 funds를 부른다. 워커가 소진될 때까지.

# 해결 — 타임아웃 + 회로차단기
두 겹으로 막았다. <br>

## 1. 타임아웃
`RestClient`에 connect/read 타임아웃을 2초로 걸었다. 이제 funds가 응답 없으면 무한 대기가 아니라 2초 만에 실패한다. <br>
그런데 타임아웃만으론 부족하다. funds가 계속 죽어있으면 **모든 요청이 여전히 2초씩 기다렸다가 실패**한다. 주문이 쏟아지면 워커는 그 2초 동안 또 묶인다. 무한 대기가 2초 대기로 바뀌었을 뿐이다. <br>

## 2. 회로차단기 (Resilience4j)
그래서 회로차단기를 얹었다. **실패가 일정 비율을 넘으면 funds 호출 자체를 끊는다.** <br>
`FundsClient`의 funds 호출에 `@CircuitBreaker(name="funds")`를 달고, 실패 시 `FUNDS_UNAVAILABLE`(503)로 fallback 하게 했다. <br>

상태 세 개로 돈다. <br>
1. **CLOSED(정상)** — funds를 정상 호출하며 실패율을 지켜본다.
2. **OPEN(차단)** — 최근 호출의 50%가 실패하면 열린다. 이후 호출은 funds에 가지도 않고 **즉시 fallback**(503)한다. 워커가 funds 대기로 묶이지 않는다.
3. **HALF_OPEN(시험)** — 10초 뒤 몇 개만 시험 호출한다. 되면 CLOSED로 닫고, 안 되면 다시 OPEN.

설정은 이렇게 뒀다(application.yml). <br>
- 슬라이딩 윈도우 20콜, 실패율 50% 넘으면 OPEN
- 2초 넘는 호출(slow call)도 실패로 간주 — funds가 죽은 게 아니라 느려지는 경우까지 잡으려고
- OPEN 유지 10초 뒤 HALF_OPEN 자동 전환

한 가지 구분을 뒀다. **funds의 4xx(잔액 부족 같은 업무 오류)는 회로에 카운트하지 않는다.** 4xx는 funds가 살아있다는 뜻이라, 이걸 실패로 세면 정상적인 주문 거절에 회로가 잘못 열린다. 타임아웃·연결 실패·5xx만 회로 실패로 본다. <br>

# 검증 — 같은 장애, 다른 결과
문제를 재현했던 것과 똑같이(funds `docker pause` + 250 주문) 측정했다. <br>

| funds 정지 시 (250 주문) | 회로차단기 없음 | 회로차단기 있음 |
|---|---|---|
| 완료된 주문 | 0 (무한 대기) | 250 (즉시 503) |
| Tomcat 워커 | 200/200 funds에 묶임 | 0개 묶임 |
| trading health | 타임아웃(먹통) | 200 / 7ms |
| compose 헬스체크 | unhealthy | healthy 유지 |
| funds 복구 후 | funds가 살아나야만 회복 | 회로가 스스로 OPEN→HALF_OPEN→CLOSED |

funds가 죽어도 **trading은 살아있고**, 주문은 무한 대기 대신 **즉시 거절(503)**된다. 워커가 묶이지 않으니 다른 요청도 정상 처리한다. <br>
funds를 복구하니 회로가 10초 뒤 HALF_OPEN으로 시험 호출을 보내고, 성공하자 CLOSED로 닫혔다. 사람 손 없이 자동 회복이다. <br>

# 정리
> 서비스를 나누자 "한 서비스가 죽으면 부르는 쪽도 죽는" 연쇄붕괴가 새로 생겼다. funds를 멈추니 trading 워커 200개가 전부 funds 대기로 묶여 trading이 통째로 먹통이 됐다. <br>
> 타임아웃(무한 대기 → 2초)과 회로차단기(실패가 쌓이면 호출 자체 차단)를 얹자, 같은 장애에서도 trading은 살아있고 주문은 즉시 거절되며 funds 복구 시 스스로 회복했다. <br>
> 회로차단기는 funds를 고치는 게 아니라 **부르는 쪽(trading)을 지키는** 장치다. funds를 되살리는 건 오케스트레이터(k8s)의 몫이고, 그 사이 trading이 같이 죽지 않게 하는 게 회로차단기의 몫이다. <br>
