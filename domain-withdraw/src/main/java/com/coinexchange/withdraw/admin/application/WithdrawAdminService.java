package com.coinexchange.withdraw.admin.application;

import com.coinexchange.events.withdraw.WithdrawApprovedEvent;
import com.coinexchange.events.withdraw.WithdrawRejectedEvent;
import com.coinexchange.withdraw.domain.Withdraw;
import com.coinexchange.withdraw.domain.repository.WithdrawRepository;
import com.coinexchange.withdraw.exception.WithdrawException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.withdraw.exception.WithdrawExceptionType.WITHDRAW_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class WithdrawAdminService {

    private final WithdrawRepository withdrawRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approve(Long withdrawId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.approve();
        withdrawRepository.save(withdraw);

        eventPublisher.publishEvent(new WithdrawApprovedEvent(
                withdraw.getUserId(),
                withdraw.getAmount(),
                withdraw.getId()
        ));
    }

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
    }
}
