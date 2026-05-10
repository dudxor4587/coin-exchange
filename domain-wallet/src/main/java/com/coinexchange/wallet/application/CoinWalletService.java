package com.coinexchange.wallet.application;

import com.coinexchange.wallet.domain.CoinWallet;
import com.coinexchange.wallet.domain.repository.CoinWalletRepository;
import com.coinexchange.wallet.exception.CoinWalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.wallet.exception.CoinWalletExceptionType.COIN_WALLET_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinWalletService {

    private final CoinWalletRepository coinWalletRepository;

    @Transactional
    public void debitCoin(Long userId, Long coinId, Long amount) {
        CoinWallet coinWallet = coinWalletRepository.findByUserIdAndCoinIdForUpdate(userId, coinId)
                .orElseThrow(() -> new CoinWalletException(COIN_WALLET_NOT_FOUND));
        coinWallet.decreaseAmount(amount);
        coinWalletRepository.save(coinWallet);
        log.info("코인 차감: userId={}, coinId={}, amount={}", userId, coinId, amount);
    }

    @Transactional
    public void creditCoin(Long userId, Long coinId, Long amount) {
        CoinWallet coinWallet = coinWalletRepository.findByUserIdAndCoinIdForUpdate(userId, coinId)
                .orElseGet(() -> CoinWallet.builder()
                        .userId(userId)
                        .coinId(coinId)
                        .amount(0L)
                        .build());
        coinWallet.increaseAmount(amount);
        coinWalletRepository.save(coinWallet);
        log.info("코인 증가: userId={}, coinId={}, amount={}", userId, coinId, amount);
    }
}
