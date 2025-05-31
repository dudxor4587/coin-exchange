package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.OrderMatchedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.ORDER_MATCHED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.ORDER_MATCHED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class OrderMatchedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderMatchedEvent event) {
        rabbitTemplate.convertAndSend(
                ORDER_MATCHED_EXCHANGE,
                ORDER_MATCHED_ROUTING_KEY,
                event
        );
    }
}
