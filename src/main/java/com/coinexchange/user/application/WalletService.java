package com.coinexchange.user.application;

import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.Wallet;
import com.coinexchange.user.domain.repository.UserRepository;
import com.coinexchange.user.domain.repository.WalletRepository;
import com.coinexchange.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.coinexchange.user.exception.UserExceptionType.USER_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public void processDeposit(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, Wallet.Currency.KRW)
                .orElseGet(() -> Wallet.builder()
                        .user(user)
                        .currency(Wallet.Currency.KRW)
                        .build());

        wallet.increaseBalance(amount);
        walletRepository.save(wallet);

        log.info("지갑 잔액 갱신 완료: userId={}, 변경된 잔액={}", userId, wallet.getBalance());
    }
}
