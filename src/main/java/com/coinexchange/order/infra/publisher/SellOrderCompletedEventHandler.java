package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.SellOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_COMPLETED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_COMPLETED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class SellOrderCompletedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(SellOrderCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                SELL_ORDER_COMPLETED_EXCHANGE,
                SELL_ORDER_COMPLETED_ROUTING_KEY,
                event
        );
    }
}
