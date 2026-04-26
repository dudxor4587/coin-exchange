package com.coinexchange.withdraw.admin.infra.publisher;

import com.coinexchange.events.withdraw.WithdrawApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQChannels.*;

@Component
@RequiredArgsConstructor
public class WithdrawApprovedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(WithdrawApprovedEvent event) {
        rabbitTemplate.convertAndSend(
                WITHDRAW_APPROVE_EXCHANGE,
                WITHDRAW_APPROVE_ROUTING_KEY,
                event
        );
    }
}
