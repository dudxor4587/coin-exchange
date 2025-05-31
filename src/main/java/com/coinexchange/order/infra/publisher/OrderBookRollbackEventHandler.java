package com.coinexchange.order.infra.publisher;

import com.coinexchange.order.event.OrderBookRollbackEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.ORDER_BOOK_ROLLBACK_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.ORDER_BOOK_ROLLBACK_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class OrderBookRollbackEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void publish(OrderBookRollbackEvent event) {
        rabbitTemplate.convertAndSend(
                ORDER_BOOK_ROLLBACK_EXCHANGE,
                ORDER_BOOK_ROLLBACK_ROUTING_KEY,
                event
        );
    }
}
