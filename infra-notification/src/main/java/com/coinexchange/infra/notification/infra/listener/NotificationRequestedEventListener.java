package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.infra.notification.application.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQChannels.NOTIFICATION_REQUESTED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestedEventListener {

    private final NotificationSender notificationSender;

    @RabbitListener(queues = NOTIFICATION_REQUESTED_QUEUE)
    public void handle(NotificationRequestedEvent event) {
        log.info("알림 요청 수신: userId={}", event.userId());
        notificationSender.send(event.userId(), event.message());
    }
}
