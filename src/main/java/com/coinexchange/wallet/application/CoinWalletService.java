package com.coinexchange.wallet.application;

import com.coinexchange.infra.notification.application.NotificationService;
import com.coinexchange.order.event.SellOrderReadyEvent;
import com.coinexchange.wallet.domain.CoinWallet;
import com.coinexchange.wallet.domain.repository.CoinWalletRepository;
import com.coinexchange.wallet.exception.CoinWalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.wallet.exception.CoinWalletExceptionType.COIN_WALLET_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinWalletService {

    private final CoinWalletRepository coinWalletRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processSellOrder(Long userId, Long orderId, Long coinId, BigDecimal price, Long amount) {
        CoinWallet coinWallet = coinWalletRepository.findByUserIdAndCoinIdForUpdate(userId, coinId)
                .orElseThrow(() -> new CoinWalletException(COIN_WALLET_NOT_FOUND));

        coinWallet.decreaseAmount(amount);

        coinWalletRepository.save(coinWallet);

        log.info("매도 주문 처리 완료: userId={}, coinId={}, amount={}", userId, coinId, amount);
        notificationService.sendSellOrderNotification(userId, amount);

        eventPublisher.publishEvent(new SellOrderReadyEvent(
                orderId,
                userId,
                coinId,
                price,
                amount
        ));
    }

    @Transactional
    public void processBuyOrderCompletion(Long userId, Long coinId, Long amount) {
        log.info("매수 주문 완료 처리: userId={}, coinId={}, amount={}", userId, coinId, amount);
        notificationService.sendBuyOrderCompletionNotification(userId, coinId, amount);
    }

    @Transactional
    public void processSellOrderCompletion(Long userId, Long coinId, Long amount) {
        log.info("매도 주문 완료 처리: userId={}, coinId={}, amount={}", userId, coinId, amount);
        notificationService.sendSellOrderCompletionNotification(userId, coinId, amount);
    }

    @Transactional
    public void processBuyOrderFill(Long userId, Long coinId, Long amount) {
        CoinWallet coinWallet = coinWalletRepository.findByUserIdAndCoinIdForUpdate(userId, coinId)
                .orElseGet(() -> CoinWallet.builder()
                        .userId(userId)
                        .coinId(coinId)
                        .amount(0L)
                        .build());

        coinWallet.increaseAmount(amount);

        coinWalletRepository.save(coinWallet);

        log.info("매수 주문 체결 완료: userId={}, coinId={}, amount={}", userId, coinId, amount);
        notificationService.sendBuyOrderFillNotification(userId, coinId, amount);
    }
}
