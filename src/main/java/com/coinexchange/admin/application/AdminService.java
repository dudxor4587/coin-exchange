package com.coinexchange.admin.application;

import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.deposit.event.DepositApprovedEvent;
import com.coinexchange.deposit.event.DepositRejectedEvent;
import com.coinexchange.deposit.exception.DepositException;
import com.coinexchange.withdraw.domain.Withdraw;
import com.coinexchange.withdraw.domain.repository.WithdrawRepository;
import com.coinexchange.withdraw.event.WithdrawApprovedEvent;
import com.coinexchange.withdraw.event.WithdrawRejectedEvent;
import com.coinexchange.withdraw.exception.WithdrawException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_NOT_FOUND;
import static com.coinexchange.withdraw.exception.WithdrawExceptionType.WITHDRAW_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DepositRepository depositRepository;
    private final WithdrawRepository withdrawRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approveDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.approve();

        depositRepository.save(deposit);

        eventPublisher.publishEvent(new DepositApprovedEvent(
                deposit.getUser().getId(),
                deposit.getAmount()
        ));
    }

    @Transactional
    public void rejectDeposit(Long depositId, String reason) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.reject(reason);

        depositRepository.save(deposit);

        eventPublisher.publishEvent(new DepositRejectedEvent(
                deposit.getUser().getId(),
                reason
        ));
    }

    @Transactional
    public void approveWithdraw(Long withdrawId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.approve();

        withdrawRepository.save(withdraw);

        eventPublisher.publishEvent(new WithdrawApprovedEvent(
                withdraw.getUser().getId(),
                withdraw.getAmount(),
                withdraw.getId()
        ));
    }

    public void rejectWithdraw(Long withdrawId, String reason) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.reject(reason);

        withdrawRepository.save(withdraw);

        eventPublisher.publishEvent(new WithdrawRejectedEvent(
                withdraw.getUser().getId(),
                reason,
                withdraw.getId()
        ));
    }
}
