package com.coinexchange.trading.infra;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQChannels.NOTIFICATION_REQUESTED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQChannels.NOTIFICATION_REQUESTED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class NotificationRequestedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificationRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                NOTIFICATION_REQUESTED_EXCHANGE,
                NOTIFICATION_REQUESTED_ROUTING_KEY,
                event
        );
    }
}
