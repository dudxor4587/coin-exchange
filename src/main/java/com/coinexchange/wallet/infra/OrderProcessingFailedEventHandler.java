package com.coinexchange.wallet.infra;

import com.coinexchange.order.event.OrderProcessingFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.ORDER_PROCESSING_FAILED_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.ORDER_PROCESSING_FAILED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class OrderProcessingFailedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void publish(OrderProcessingFailedEvent event) {
        rabbitTemplate.convertAndSend(
                ORDER_PROCESSING_FAILED_EXCHANGE,
                ORDER_PROCESSING_FAILED_ROUTING_KEY,
                event
        );
    }
}
