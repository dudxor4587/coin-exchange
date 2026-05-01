package com.coinexchange.wallet.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.events.order.BuyOrderReadyEvent;
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

        walletLogger(userId, amount);
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

        walletLogger(userId, amount);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                userId,
                "거래소에 요청하신 출금 처리가 완료되었습니다. 출금액: " + amount
        ));
    }

    private void walletLogger(Long userId, BigDecimal amount) {
        log.info("지갑 처리 완료: userId={}, amount={}", userId, amount);
    }

    @Transactional
    public void processBuyOrder(Long userId, BigDecimal lockedFunds, Long orderId, Long coinId, BigDecimal price, Long amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));

        wallet.decreaseBalance(lockedFunds);

        walletRepository.save(wallet);

        log.info("매수 주문 처리 완료: userId={}, lockedFunds={}", userId, lockedFunds);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                userId,
                "거래소에 요청하신 매수 주문이 완료되었습니다. 주문 금액: " + lockedFunds
        ));

        eventPublisher.publishEvent(new BuyOrderReadyEvent(
                orderId,
                userId,
                coinId,
                lockedFunds,
                price,
                amount
        ));
    }

    @Transactional
    public void processSellOrderFill(Long userId, BigDecimal price) {
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, Wallet.Currency.KRW)
                .orElseThrow(() -> new WalletException(WALLET_NOT_FOUND));

        wallet.increaseBalance(price);

        walletRepository.save(wallet);

        log.info("매도 주문 체결 완료: userId={}, price={}", userId, price);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                userId,
                String.format("매도 주문 체결 완료: 사용자 ID: %d, 체결 금액: %s", userId, price)
        ));
    }
}
