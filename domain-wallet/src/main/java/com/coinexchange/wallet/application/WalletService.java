package com.coinexchange.wallet.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.wallet.domain.Wallet;
import com.coinexchange.wallet.domain.repository.WalletRepository;
import com.coinexchange.wallet.exception.WalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.wallet.exception.WalletExceptionType.WALLET_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processDeposit(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseGet(() -> Wallet.builder()
                        .userId(userId)
                        .currency(Wallet.Currency.KRW)
                        .build());

        wallet.increaseBalance(amount);
        walletRepository.save(wallet);

        log.info("입금 처리: userId={}, amount={}", userId, amount);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                userId,
                "거래소에 요청하신 입금 처리가 완료되었습니다. 입금액: " + amount
        ));
    }

    @Transactional
    public void processWithdraw(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));

        wallet.decreaseBalance(amount);
        walletRepository.save(wallet);

        log.info("출금 처리: userId={}, amount={}", userId, amount);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                userId,
                "거래소에 요청하신 출금 처리가 완료되었습니다. 출금액: " + amount
        ));
    }

    @Transactional
    public void debitKrw(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));
        wallet.decreaseBalance(amount);
        walletRepository.save(wallet);
        log.info("KRW 차감: userId={}, amount={}", userId, amount);
    }

    @Transactional
    public void creditKrw(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseGet(() -> Wallet.builder()
                        .userId(userId)
                        .currency(Wallet.Currency.KRW)
                        .build());
        wallet.increaseBalance(amount);
        walletRepository.save(wallet);
        log.info("KRW 증가: userId={}, amount={}", userId, amount);
    }
}
