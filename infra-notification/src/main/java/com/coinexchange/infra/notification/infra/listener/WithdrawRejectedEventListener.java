package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.withdraw.WithdrawRejectedEvent;
import com.coinexchange.infra.notification.application.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQChannels.WITHDRAW_REJECT_QUEUE;

@RequiredArgsConstructor
@Component
@Slf4j
public class WithdrawRejectedEventListener {

    private final NotificationSender notificationSender;

    @RabbitListener(queues = WITHDRAW_REJECT_QUEUE)
    public void handleWithdrawRejected(WithdrawRejectedEvent event) {
        log.info("출금 거절 이벤트 수신: userId={}, reason={}", event.userId(), event.reason());
        notificationSender.send(
                event.userId(),
                "거래소에 요청하신 출금이 거절되었습니다. 사유: " + event.reason()
        );
    }
}
