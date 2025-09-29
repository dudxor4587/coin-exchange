package com.coinexchange.order.infra.publisher;

import com.coinexchange.trade.event.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.TRADE_CREATED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.TRADE_CREATED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class TradeCreatedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void publish(TradeCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                TRADE_CREATED_EXCHANGE,
                TRADE_CREATED_ROUTING_KEY,
                event
        );
    }
}
