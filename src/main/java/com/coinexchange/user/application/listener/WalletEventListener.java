package com.coinexchange.user.application.listener;

import com.coinexchange.deposit.event.DepositApprovedEvent;

import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_APPROVE_QUEUE;
import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_APPROVE_QUEUE;

import com.coinexchange.user.application.WalletService;
import com.coinexchange.user.exception.WalletException;
import com.coinexchange.withdraw.event.WithdrawApprovedEvent;
import com.coinexchange.withdraw.event.WithdrawFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = DEPOSIT_APPROVE_QUEUE)
    public void handleDepositApproved(DepositApprovedEvent event) {
        log.info("입금 승인 이벤트 수신: userId={}, amount={}", event.userId(), event.amount());
        walletService.processDeposit(event.userId(), event.amount());
    }

    @RabbitListener(queues = WITHDRAW_APPROVE_QUEUE)
    public void handleWithdrawApproved(WithdrawApprovedEvent event) {
        log.info("출금 승인 이벤트 수신: userId={}, amount={}", event.userId(), event.amount());
        try {
            walletService.processWithdraw(event.userId(), event.amount());
        } catch (WalletException e) {
            log.warn("출금 처리 실패: {}", e.getMessage());
            eventPublisher.publishEvent(new WithdrawFailedEvent(
                    e.getMessage(),
                    event.withdrawId()
            ));
        }
    }
}
