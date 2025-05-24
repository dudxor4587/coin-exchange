package com.coinexchange.user.application;

import com.coinexchange.notification.application.NotificationService;
import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.Wallet;
import com.coinexchange.user.domain.repository.UserRepository;
import com.coinexchange.user.domain.repository.WalletRepository;
import com.coinexchange.user.exception.UserException;
import com.coinexchange.user.exception.WalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.user.exception.UserExceptionType.USER_NOT_FOUND;
import static com.coinexchange.user.exception.WalletExceptionType.WALLET_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void processDeposit(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseGet(() -> Wallet.builder()
                        .user(user)
                        .currency(Wallet.Currency.KRW)
                        .build());

        wallet.increaseBalance(amount);
        walletRepository.save(wallet);

        walletLogger(userId, amount);
        notificationService.sendDepositNotification(userId, amount);
    }

    @Transactional
    public void processWithdraw(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));

        wallet.decreaseBalance(amount);
        walletRepository.save(wallet);

        walletLogger(userId, amount);
        notificationService.sendWithdrawNotification(userId, amount);
    }

    private void walletLogger(Long userId, BigDecimal amount) {
        log.info("지갑 처리 완료: userId={}, amount={}", userId, amount);
    }
}
