package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.SellOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_CREATED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_CREATED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class SellOrderCreatedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(SellOrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                SELL_ORDER_CREATED_EXCHANGE,
                SELL_ORDER_CREATED_ROUTING_KEY,
                event
        );
    }
}
