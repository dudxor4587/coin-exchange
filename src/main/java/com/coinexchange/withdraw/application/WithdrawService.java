package com.coinexchange.withdraw.application;

import com.coinexchange.notification.application.NotificationSender;
import com.coinexchange.withdraw.application.dto.WithdrawResponse;
import com.coinexchange.withdraw.application.mapper.WithdrawMapper;
import com.coinexchange.withdraw.domain.Withdraw;
import com.coinexchange.withdraw.domain.repository.WithdrawRepository;
import com.coinexchange.withdraw.exception.WithdrawException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.withdraw.exception.WithdrawExceptionType.WITHDRAW_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final WithdrawRepository withdrawRepository;
    private final NotificationSender notificationSender;

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

    @Transactional
    public void handleWithdrawFailure(Long withdrawId, String reason) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.fail(reason);
        withdrawRepository.save(withdraw);

        notificationSender.send(
                withdraw.getUserId(),
                "출금 실패, 사유 : " + reason
        );
    }
}
