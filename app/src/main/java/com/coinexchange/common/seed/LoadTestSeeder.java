package com.coinexchange.common.seed;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.coin.domain.repository.CoinRepository;
import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CoinWalletRepository coinWalletRepository;
    private final CoinRepository coinRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("부하 테스트 시드 데이터 이미 존재 - 스킵");
            return;
        }

        User buyer = userRepository.save(User.builder()
                .email("buyer@test.com")
                .password("test")
                .name("buyer")
                .phone("010-0000-0001")
                .role(User.Role.USER)
                .build());

        User seller = userRepository.save(User.builder()
                .email("seller@test.com")
                .password("test")
                .name("seller")
                .phone("010-0000-0002")
                .role(User.Role.USER)
                .build());

        Coin coin = coinRepository.save(Coin.builder()
                .symbol(Coin.Symbol.BTC)
                .name("Bitcoin")
                .build());

        seedWallet(buyer.getId());
        seedWallet(seller.getId());
        seedCoinWallet(buyer.getId(), coin.getId());
        seedCoinWallet(seller.getId(), coin.getId());

        log.info("부하 테스트 시드 완료: buyer={}, seller={}, coin={}",
                buyer.getId(), seller.getId(), coin.getId());
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
