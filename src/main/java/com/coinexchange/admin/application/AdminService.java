package com.coinexchange.admin.application;

import com.coinexchange.admin.infra.publisher.DepositApprovedEventPublisher;
import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.deposit.exception.DepositException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DepositRepository depositRepository;
    private final DepositApprovedEventPublisher eventPublisher;

    @Transactional
    public void approveDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DEPOSIT_NOT_FOUND));

        deposit.approve();

        eventPublisher.publish(deposit);

        depositRepository.save(deposit);
    }

}
