package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.infra.notification.application.NotificationService;
import com.coinexchange.withdraw.event.WithdrawRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_REJECT_QUEUE;

@RequiredArgsConstructor
@Component
@Slf4j
public class WithdrawRejectedEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = WITHDRAW_REJECT_QUEUE)
    public void handleDepositRejected(WithdrawRejectedEvent event) {
        log.info("출금 거절 이벤트 수신: userId={}, reason={}", event.userId(), event.reason());
        notificationService.sendWithdrawRejectionNotification(event.userId(), event.reason());
    }
}
