package com.coinexchange.admin.infra.publisher;

import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.event.DepositApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class DepositApprovedEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(Deposit deposit) {
        DepositApprovedEvent event = new DepositApprovedEvent(deposit.getUser().getId(), deposit.getAmount());
        rabbitTemplate.convertAndSend(
                DEPOSIT_EXCHANGE,
                DEPOSIT_ROUTING_KEY,
                event
        );
    }
}
