package com.coinexchange.admin.infra.publisher.impl;

import com.coinexchange.admin.application.publisher.DepositApprovedEventPublisher;
import com.coinexchange.deposit.event.DepositApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.*;

@Component
@RequiredArgsConstructor
public class DepositApprovedEventPublisherImpl implements DepositApprovedEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DepositApprovedEvent event) {
        rabbitTemplate.convertAndSend(
                DEPOSIT_APPROVE_EXCHANGE,
                DEPOSIT_APPROVE_ROUTING_KEY,
                event
        );
    }
}
