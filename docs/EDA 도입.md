EDA라는 것을 이미 알고는 있었지만 어떤 상황에 적용해야 하는지, 왜 적용해야하는지는 알 수 없었다. <br>
코인 거래소를 구현하며 들었던 고민과 어떻게 해서 도입을 하게 되었는지를 정리해보고자 한다.

# EDA란?
> EDA는 Event Driven Architecture의 약자로, 이벤트 기반 아키텍처를 의미한다. <br>
이벤트 기반 아키텍처는 시스템의 구성 요소들이 서로 독립적으로 동작하고, 이벤트를 통해 상호작용하는 구조를 말한다. <br>
말이 조금 어렵지만, 단순하게 이벤트를 통해 시스템의 구성 요소들이 서로 통신하는 구조라고 생각하면 된다.

# 기존 로직의 문제점
우선 처음부터 EDA를 도입하려고 한 것은 아니다.
입금 기능을 구현할 때 플로우를 생각해보자.
1. 사용자가 입금을 요청한다.
2. 관리자가 입금 요청을 처리한다.
3. 입금이 완료되면 사용자의 잔고를 업데이트한다.
4. 입금이 완료되었다는 알림을 사용자에게 전송한다.

정도로 볼 수 있다. <br>
이 플로우를 구현하기 위해 서비스 단에서의 코드를 예시로 짜보자. <br>
빠른 진행을 위해 관리자가 무조건 승인한다는 가정하에 작성하였다. <br>

```java
public class AdminService {

    private final WalletRepository walletRepository;
    private final NotificationService notificationService;
    private final DepositRequestRepository depositRequestRepository;

    // 사용자의 입금 요청을 처리하는 메서드
    @Transactional
    public void deposit(DepositRequest request) {
        Deposit deposit = depositRepository.findById(request.getDepositId())
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));
        // 1. 관리자가 입금 요청을 처리한다.
        deposit.approve();
        depositRepository.save(deposit);

        // 2. 입금이 완료되면 사용자의 잔고를 업데이트한다.
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));
        
        wallet.addBalance(deposit.getAmount());
        walletRepository.save(wallet);

        // 3. 입금이 완료되었다는 알림을 사용자에게 전송한다.
        notificationService.sendNotification(request.getUserId(), "입금이 완료되었습니다.");
    }
}
```
이렇게 작성하면, 같은 트랜잭션 단위로 묶여서 처리된다. <br>
이 경우의 장점에 대해서 생각해보자. <br>
1. 중간에 에러가 발생하면 롤백이 가능하다.
2. 트랜잭션 단위로 처리되기 때문에 데이터의 일관성을 유지할 수 있다.

정도의 장점이 있다. <br>
하지만 이 경우에는 단점도 존재한다. <br>
1. 치명적인 에러가 아니더라도 롤백이 발생할 수 있다.
2. 대용량 데이터를 처리할 때 성능 저하가 발생할 수 있다.

우선, "입금"이라는 비지니스 로직의 특성 상, 정합성이 중요하다. <br>
사용자가 입금을 요청했는데, 입금 요청 자체는 수락되고 사용자의 잔고가 업데이트 되지 않으면 큰 문제가 된다. <br>
이 관점에서 보면, 같은 트랜잭션 단위로 묶어서 처리하는 것이 좋아 보인다. <br>
하지만 "입금"이라는 비지니스 로직은 사용자가 요청하는 경우가 많고, 그에 따라 대량의 데이터가 발생할 수 있다. <br>
같은 트랜잭션 단위로 묶여서 정합성은 유지되지만, 그만큼 성능 저하가 발생할 수 있다. <br>

## 트랜잭션 단위로 묶인 입금 요청이 대용량 처리에서 왜 성능 저하를 발생시키는가?
트랜잭션은 처리 중인 동안 DB 커넥션과 잠금(Lock) 등 자원을 점유하게 된다. <br>
요청이 적을 때에는 크게 문제가 되지 않지만, 수백, 수천 건의 입금 요청이 동시에 들어오면, 각 요청마다 트랜잭션이 생성되고 자원을 점유하게 된다. <br>
이 요청이 동시에 들어온다면 커넥션 풀이 고갈될 수 있다. <br>
또한 트랜잭션이 롤백되더라도, 롤백이 완료되기 전까지는 해당 트랜잭션이 사용한 자원은 점유되어 있기 때문에 다른 요청들이 지연되거나 재시도를 하게 된다. <br>

이러한 문제는 대량의 데이터가 발생하는 "입금"이라는 비지니스 로직에서 더욱 두드러진다. <br>
그리고, "알림 전송"에서 오류가 발생할 경우에도 트랜잭션이 롤백되게 되는데, 이 경우가 적합할까? <br>
알림 전송은 사용자의 잔고와는 관계가 없기 때문에, 알림 전송이 실패하더라도 사용자의 잔고는 업데이트 되어야 한다. <br>

# 왜 EDA를 선택해야 했는가?
사실 단순히 "입금"이라는 비지니스 로직을 처리할 때에는 EDA를 도입할 필요는 없었다. <br>
하지만 "입금"이라는 비지니스 로직을 처리하는 과정에서 발생하는 대량의 데이터와 성능 저하 문제를 해결하기 위해 EDA를 도입하게 되었다. <br>
이전 구조를 보면 각 서비스들이 서로 의존하고 있다. <br>
이러한 구조에서는 각 서비스들이 서로의 상태를 알 수 없기 때문에, 각 서비스들이 독립적으로 동작할 수 없다. <br>
그럼 EDA를 도입하면 어떻게 될까? <br>
1. 사용자가 입금을 요청한다.
2. 관리자가 입금 요청을 처리한다.
3. 입금 요청이 완료되었다는 이벤트를 발행한다.
4. 입금 요청이 완료되었다는 이벤트를 구독하는 서비스가 이벤트를 수신한다.
5. 사용자의 잔고를 업데이트한다.
6. 입금이 완료되었다는 알림을 사용자에게 전송한다.

이렇게 되면, 각 서비스들이 서로 독립적으로 동작할 수 있다. <br>
서로의 상태를 알 필요가 없이 이벤트를 통해 상호작용할 수 있다. <br>
이러한 구조에서는 각 서비스들이 최소 트랜잭션 단위로 동작할 수 있기 때문에, 대량의 데이터가 발생하더라도 대응이 가능하다. <br>

# EDA 도입 후 코드
이제 EDA를 도입한 후의 코드를 보자. <br>
```java
public class AdminService {

    private final DepositRepository depositRepository;
    private final DepositApprovedEventPublisher approvedEventPublisher;

    @Transactional
    public void approveDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.approve();

        approvedEventPublisher.publish(new DepositApprovedEvent(
                deposit.getUser().getId(),
                deposit.getAmount()
        ));

        depositRepository.save(deposit);
    }
}
```
관리자는 그저 입금 요청을 승인하고, 이벤트를 발행하는 역할만 한다. <br>
이벤트를 수신하는 곳에선 사용자의 잔고를 업데이트하고 알림을 전송하는 역할을 한다. <br>

# 문제점과 해결 방법

결국 정합성이 가장 문제였다. <br>
정합성을 유지하기 위해서는 이벤트를 발행한 후, 사용자의 잔고를 업데이트하고 알림을 전송하는 과정에서 문제가 발생하면 롤백이 되어야 한다. <br>
하지만 EDA에서는 각 서비스들이 독립적으로 동작하기 때문에, 롤백이 불가능하다. <br>
이러한 문제를 해결하기 위해서는 어떻게 해야 할까? <br> <br>
다행히도 Spring AMQP에서는 이벤트 처리가 실패할 경우, 해당 이벤트를 다시 발행하는 기능을 제공한다. <br>
이 기능을 활용하면, 이벤트를 발행한 후 사용자의 잔고를 업데이트하고 알림을 전송하는 과정에서 문제가 발생하면, 해당 이벤트를 다시 발행하여 처리할 수 있다. <br>
여기서도 문제가 발생할 수 있는데, **멱등성**을 보장해야 한다. <br>
**멱등성**이란, 같은 요청을 여러 번 보내더라도 결과가 같아야 한다는 것이다. <br>
예를 들어 사용자의 잔고를 업데이트하는 과정에서 문제가 발생하면, 해당 이벤트가 계속 다시 발행되는데, 이때 잔고가 중복되어 업데이트 될 수도 있다. <br>
이러한 문제를 해결하기 위해, 중요 로직에서 멱등성을 보장하도록 구현해야 한다. <br>
<br>

> EDA는 각 서비스들이 독립적으로 동작할 수 있다는 장점이 있다. <br>
> 현재는 큰 단위의 상호작용이 일어나지 않기에 체감이 되지 않지만, 매수/매도의 경우 여러 서비스들의 상호작용이 필요하다. <br>
> 해당 기능을 구현할 때에는 EDA를 도입하게 된 이유를 크게 체감할 수 있을 것 같다.
