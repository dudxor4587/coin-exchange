package com.coinexchange.order.infra.publisher;

import com.coinexchange.trade.event.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.TRADE_CREATED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.TRADE_CREATED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class TradeCreatedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TradeCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                TRADE_CREATED_EXCHANGE,
                TRADE_CREATED_ROUTING_KEY,
                event
        );
    }
}
