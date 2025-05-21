package com.coinexchange.admin.infra.publisher.impl;

import com.coinexchange.admin.application.publisher.DepositRejectedEventPublisher;
import com.coinexchange.deposit.event.DepositRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_REJECT_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_REJECT_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class DepositRejectedEventPublisherImpl implements DepositRejectedEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DepositRejectedEvent event) {
        rabbitTemplate.convertAndSend(
                DEPOSIT_REJECT_EXCHANGE,
                DEPOSIT_REJECT_ROUTING_KEY,
                event
        );
    }
}
