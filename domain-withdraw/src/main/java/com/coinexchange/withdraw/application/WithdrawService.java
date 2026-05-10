package com.coinexchange.withdraw.application;

import com.coinexchange.withdraw.application.dto.WithdrawResponse;
import com.coinexchange.withdraw.application.mapper.WithdrawMapper;
import com.coinexchange.withdraw.domain.Withdraw;
import com.coinexchange.withdraw.domain.repository.WithdrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final WithdrawRepository withdrawRepository;

    @Transactional
    public WithdrawResponse withdrawRequest(Long userId, String bank, String accountNumber, BigDecimal amount) {
        Withdraw withdraw = Withdraw.builder()
                .userId(userId)
                .bank(bank)
                .accountNumber(accountNumber)
                .amount(amount)
                .status(Withdraw.Status.PENDING)
                .build();

        withdrawRepository.save(withdraw);
        return WithdrawMapper.toResponse(withdraw);
    }
}
