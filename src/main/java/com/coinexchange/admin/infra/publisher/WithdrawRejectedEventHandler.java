package com.coinexchange.admin.infra.publisher;

import com.coinexchange.withdraw.event.WithdrawRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_REJECT_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_REJECT_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class WithdrawRejectedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(WithdrawRejectedEvent event) {
        rabbitTemplate.convertAndSend(
                WITHDRAW_REJECT_EXCHANGE,
                WITHDRAW_REJECT_ROUTING_KEY,
                event
        );
    }
}
