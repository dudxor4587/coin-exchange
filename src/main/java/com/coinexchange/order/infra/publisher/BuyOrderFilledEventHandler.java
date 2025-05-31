package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.BuyOrderFilledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.BUY_ORDER_FILLED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.BUY_ORDER_FILLED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class BuyOrderFilledEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BuyOrderFilledEvent event) {
        rabbitTemplate.convertAndSend(
                BUY_ORDER_FILLED_EXCHANGE,
                BUY_ORDER_FILLED_ROUTING_KEY,
                event
        );
    }
}
