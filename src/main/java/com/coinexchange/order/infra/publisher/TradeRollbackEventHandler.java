package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.TradeRollbackEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.TRADE_ROLLBACK_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.TRADE_ROLLBACK_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class TradeRollbackEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void publish(TradeRollbackEvent event) {
        rabbitTemplate.convertAndSend(
                TRADE_ROLLBACK_EXCHANGE,
                TRADE_ROLLBACK_ROUTING_KEY,
                event
        );
    }
}
