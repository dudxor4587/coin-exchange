package com.coinexchange.deposit.admin.application;

import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.deposit.exception.DepositException;
import com.coinexchange.events.deposit.DepositApprovedEvent;
import com.coinexchange.events.deposit.DepositRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DepositAdminService {

    private final DepositRepository depositRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approve(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.approve();
        depositRepository.save(deposit);

        eventPublisher.publishEvent(new DepositApprovedEvent(
                deposit.getUserId(),
                deposit.getAmount()
        ));
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
    }
}
