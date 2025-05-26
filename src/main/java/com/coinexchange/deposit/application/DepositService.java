package com.coinexchange.deposit.application;

import com.coinexchange.deposit.application.dto.DepositResponse;
import com.coinexchange.deposit.application.mapper.DepositMapper;
import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositRepository depositRepository;

    @Transactional
    public DepositResponse depositRequest(BigDecimal amount, String bank, String accountNumber, Long userId) {
        Deposit deposit = Deposit.builder()
                .userId(userId)
                .amount(amount)
                .bank(bank)
                .accountNumber(accountNumber)
                .status(Deposit.Status.PENDING)
                .build();

        depositRepository.save(deposit);

        return DepositMapper.toResponse(deposit);
    }
}
