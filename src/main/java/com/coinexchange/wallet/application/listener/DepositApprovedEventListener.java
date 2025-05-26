package com.coinexchange.wallet.application.listener;

import com.coinexchange.deposit.event.DepositApprovedEvent;
import com.coinexchange.wallet.application.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_APPROVE_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositApprovedEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = DEPOSIT_APPROVE_QUEUE)
    public void handleDepositApproved(DepositApprovedEvent event) {
        log.info("입금 승인 이벤트 수신: userId={}, amount={}", event.userId(), event.amount());
        walletService.processDeposit(event.userId(), event.amount());
    }
}
