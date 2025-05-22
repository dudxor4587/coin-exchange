package com.coinexchange.admin.infra.publisher;

import com.coinexchange.deposit.event.DepositApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.*;

@Component
@RequiredArgsConstructor
public class DepositApprovedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DepositApprovedEvent event) {
        rabbitTemplate.convertAndSend(
                DEPOSIT_APPROVE_EXCHANGE,
                DEPOSIT_APPROVE_ROUTING_KEY,
                event
        );
    }
}
