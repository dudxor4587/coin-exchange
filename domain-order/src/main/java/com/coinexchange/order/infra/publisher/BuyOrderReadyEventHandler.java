package com.coinexchange.order.infra.publisher;

import com.coinexchange.events.order.BuyOrderReadyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQChannels.BUY_ORDER_READY_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQChannels.BUY_ORDER_READY_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class BuyOrderReadyEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BuyOrderReadyEvent event) {
        rabbitTemplate.convertAndSend(
                BUY_ORDER_READY_EXCHANGE,
                BUY_ORDER_READY_ROUTING_KEY,
                event
        );
    }
}
