package com.coinexchange.funds.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.events.withdraw.WithdrawRejectedEvent;
import com.coinexchange.wallet.application.WalletService;
import com.coinexchange.withdraw.domain.Withdraw;
import com.coinexchange.withdraw.domain.repository.WithdrawRepository;
import com.coinexchange.withdraw.exception.WithdrawException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.withdraw.exception.WithdrawExceptionType.WITHDRAW_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawApprovalService {

    private final WithdrawRepository withdrawRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approve(Long withdrawId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.approve();
        withdrawRepository.save(withdraw);

        walletService.debitKrw(withdraw.getUserId(), withdraw.getAmount());

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                withdraw.getUserId(),
                "거래소에 요청하신 출금 처리가 완료되었습니다. 출금액: " + withdraw.getAmount()
        ));
        log.info("출금 승인 완료: withdrawId={}, userId={}, amount={}",
                withdrawId, withdraw.getUserId(), withdraw.getAmount());
    }

    @Transactional
    public void reject(Long withdrawId, String reason) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.reject(reason);
        withdrawRepository.save(withdraw);

        eventPublisher.publishEvent(new WithdrawRejectedEvent(
                withdraw.getUserId(),
                reason,
                withdraw.getId()
        ));
        log.info("출금 거절: withdrawId={}, reason={}", withdrawId, reason);
    }
}
