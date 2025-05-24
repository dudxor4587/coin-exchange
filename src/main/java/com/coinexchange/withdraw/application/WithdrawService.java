package com.coinexchange.withdraw.application;

import com.coinexchange.notification.application.NotificationSender;
import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.Wallet;
import com.coinexchange.user.domain.repository.UserRepository;
import com.coinexchange.user.domain.repository.WalletRepository;
import com.coinexchange.user.exception.UserException;
import com.coinexchange.user.exception.WalletException;
import com.coinexchange.withdraw.application.dto.WithdrawResponse;
import com.coinexchange.withdraw.application.mapper.WithdrawMapper;
import com.coinexchange.withdraw.domain.Withdraw;
import com.coinexchange.withdraw.domain.repository.WithdrawRepository;
import com.coinexchange.withdraw.exception.WithdrawException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.user.exception.UserExceptionType.USER_NOT_FOUND;
import static com.coinexchange.user.exception.WalletExceptionType.INSUFFICIENT_BALANCE;
import static com.coinexchange.user.exception.WalletExceptionType.WALLET_NOT_FOUND;
import static com.coinexchange.withdraw.exception.WithdrawExceptionType.WITHDRAW_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final WithdrawRepository withdrawRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;
    private final WalletRepository walletRepository;

    @Transactional
    public WithdrawResponse withdrawRequest(Long userId, String bank, String accountNumber, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, Wallet.Currency.KRW)
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));

        validateWithdrawal(wallet, amount);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Withdraw withdraw = Withdraw.builder()
                .user(user)
                .bank(bank)
                .accountNumber(accountNumber)
                .amount(amount)
                .status(Withdraw.Status.PENDING)
                .build();

        withdrawRepository.save(withdraw);

        return WithdrawMapper.toResponse(withdraw);
    }

    private void validateWithdrawal(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new WalletException(INSUFFICIENT_BALANCE);
        }
    }

    @Transactional
    public void handleWithdrawFailure(Long withdrawId, String reason) {
        Withdraw withdraw = withdrawRepository.findById(withdrawId)
                .orElseThrow(() -> new WithdrawException(WITHDRAW_NOT_FOUND));

        withdraw.fail(reason);
        withdrawRepository.save(withdraw);

        notificationSender.send(
                withdraw.getUser().getId(),
                "출금 실패, 사유 : " + reason
        );
    }
}
