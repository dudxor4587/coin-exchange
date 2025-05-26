package com.coinexchange.wallet.infra;

import com.coinexchange.withdraw.event.WithdrawFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_FAILURE_EXCHANGE;
import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_FAILURE_ROUTING_KEY;

@RequiredArgsConstructor
@Component
public class WithdrawFailedEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void publish(WithdrawFailedEvent event) {
        rabbitTemplate.convertAndSend(
                WITHDRAW_FAILURE_EXCHANGE,
                WITHDRAW_FAILURE_ROUTING_KEY,
                event
        );
    }
}
