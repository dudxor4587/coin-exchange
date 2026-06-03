package com.coinexchange.deposit.admin.infra.publisher;

import com.coinexchange.events.deposit.DepositRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQChannels.DEPOSIT_REJECT_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQChannels.DEPOSIT_REJECT_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class DepositRejectedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DepositRejectedEvent event) {
        rabbitTemplate.convertAndSend(
                DEPOSIT_REJECT_EXCHANGE,
                DEPOSIT_REJECT_ROUTING_KEY,
                event
        );
    }
}
