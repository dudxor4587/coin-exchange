package com.coinexchange.funds.application;

import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.deposit.exception.DepositException;
import com.coinexchange.events.deposit.DepositRejectedEvent;
import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.wallet.application.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositApprovalService {

    private final DepositRepository depositRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approve(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.approve();
        depositRepository.save(deposit);

        walletService.creditKrw(deposit.getUserId(), deposit.getAmount());

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                deposit.getUserId(),
                "거래소에 요청하신 입금 처리가 완료되었습니다. 입금액: " + deposit.getAmount()
        ));
        log.info("입금 승인 완료: depositId={}, userId={}, amount={}",
                depositId, deposit.getUserId(), deposit.getAmount());
    }

    @Transactional
    public void reject(Long depositId, String reason) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.reject(reason);
        depositRepository.save(deposit);

        eventPublisher.publishEvent(new DepositRejectedEvent(
                deposit.getUserId(),
                reason
        ));
        log.info("입금 거절: depositId={}, reason={}", depositId, reason);
    }
}
