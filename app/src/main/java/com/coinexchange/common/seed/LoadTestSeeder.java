package com.coinexchange.common.seed;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.coin.domain.repository.CoinRepository;
import com.coinexchange.wallet.domain.CoinWallet;
import com.coinexchange.wallet.domain.Wallet;
import com.coinexchange.wallet.domain.repository.CoinWalletRepository;
import com.coinexchange.wallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestSeeder implements CommandLineRunner {

    private static final BigDecimal SEED_KRW = new BigDecimal("100000000000");
    private static final long SEED_COIN = 100_000_000L;
    private static final Long SEED_BUYER_ID = 1L;
    private static final Long SEED_SELLER_ID = 2L;

    private final WalletRepository walletRepository;
    private final CoinWalletRepository coinWalletRepository;
    private final CoinRepository coinRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (walletRepository.count() > 0) {
            log.info("부하 테스트 시드 데이터 이미 존재 - 스킵");
            return;
        }

        Coin coin = coinRepository.save(Coin.builder()
                .symbol(Coin.Symbol.BTC)
                .name("Bitcoin")
                .build());

        seedWallet(SEED_BUYER_ID);
        seedWallet(SEED_SELLER_ID);
        seedCoinWallet(SEED_BUYER_ID, coin.getId());
        seedCoinWallet(SEED_SELLER_ID, coin.getId());

        log.info("부하 테스트 시드 완료: buyer={}, seller={}, coin={}",
                SEED_BUYER_ID, SEED_SELLER_ID, coin.getId());
    }

    private void seedWallet(Long userId) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency(Wallet.Currency.KRW)
                .build();
        wallet.increaseBalance(SEED_KRW);
        walletRepository.save(wallet);
    }

    private void seedCoinWallet(Long userId, Long coinId) {
        coinWalletRepository.save(CoinWallet.builder()
                .userId(userId)
                .coinId(coinId)
                .amount(SEED_COIN)
                .build());
    }
}
