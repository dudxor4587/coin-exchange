package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.BuyOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.*;

@Component
@RequiredArgsConstructor
public class BuyOrderCreatedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BuyOrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                BUY_ORDER_CREATED_EXCHANGE,
                BUY_ORDER_CREATED_ROUTING_KEY,
                event
        );
    }
}
