package com.coinexchange.user.application.listener;

import com.coinexchange.deposit.event.DepositApprovedEvent;

import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_APPROVE_QUEUE;

import com.coinexchange.user.application.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = DEPOSIT_APPROVE_QUEUE)
    public void handleDepositApproved(DepositApprovedEvent event) {
        log.info("입금 승인 이벤트 수신: userId={}, amount={}", event.userId(), event.amount());
        walletService.processDeposit(event.userId(), event.amount());
    }
}
