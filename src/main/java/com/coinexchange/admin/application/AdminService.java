package com.coinexchange.admin.application;

import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.deposit.event.DepositApprovedEvent;
import com.coinexchange.deposit.event.DepositRejectedEvent;
import com.coinexchange.deposit.exception.DepositException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DepositRepository depositRepository;
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
}
