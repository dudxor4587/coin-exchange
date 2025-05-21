package com.coinexchange.admin.application;

import com.coinexchange.admin.application.publisher.DepositApprovedEventPublisher;
import com.coinexchange.admin.application.publisher.DepositRejectedEventPublisher;
import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.deposit.event.DepositApprovedEvent;
import com.coinexchange.deposit.event.DepositRejectedEvent;
import com.coinexchange.deposit.exception.DepositException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DepositRepository depositRepository;
    private final DepositApprovedEventPublisher approvedEventPublisher;
    private final DepositRejectedEventPublisher rejectedEventPublisher;

    @Transactional
    public void approveDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.approve();

        approvedEventPublisher.publish(new DepositApprovedEvent(
                deposit.getUser().getId(),
                deposit.getAmount()
        ));

        depositRepository.save(deposit);
    }

    @Transactional
    public void rejectDeposit(Long depositId, String reason) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.reject(reason);

        rejectedEventPublisher.publish(new DepositRejectedEvent(
                deposit.getUser().getId(),
                reason
        ));

        depositRepository.save(deposit);
    }
}
