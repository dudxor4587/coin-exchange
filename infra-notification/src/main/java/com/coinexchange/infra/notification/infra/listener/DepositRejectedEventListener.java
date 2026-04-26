package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.deposit.DepositRejectedEvent;
import com.coinexchange.infra.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQChannels.DEPOSIT_REJECT_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class DepositRejectedEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = DEPOSIT_REJECT_QUEUE)
    public void handleDepositRejected(DepositRejectedEvent event) {
        log.info("입금 거절 이벤트 수신: userId={}, reason={}", event.userId(), event.reason());
        notificationService.sendDepositRejectionNotification(event.userId(), event.reason());
    }
}
