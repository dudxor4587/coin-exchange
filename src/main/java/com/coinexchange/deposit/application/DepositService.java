package com.coinexchange.deposit.application;

import com.coinexchange.deposit.application.dto.DepositResponse;
import com.coinexchange.deposit.application.mapper.DepositMapper;
import com.coinexchange.deposit.domain.Deposit;
import com.coinexchange.deposit.domain.repository.DepositRepository;
import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.repository.UserRepository;
import com.coinexchange.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.user.exception.UserExceptionType.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositRepository depositRepository;
    private final UserRepository userRepository;

    @Transactional
    public DepositResponse depositRequest(BigDecimal amount, String bank, String accountNumber, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Deposit deposit = Deposit.builder()
                .user(user)
                .amount(amount)
                .bank(bank)
                .accountNumber(accountNumber)
                .status(Deposit.Status.PENDING)
                .build();

        depositRepository.save(deposit);

        return DepositMapper.toResponse(deposit);
    }
}
