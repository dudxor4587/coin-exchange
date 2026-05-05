package com.coinexchange.funds.seed;

import com.coinexchange.wallet.domain.CoinWallet;
import com.coinexchange.wallet.domain.Wallet;
import com.coinexchange.wallet.domain.repository.CoinWalletRepository;
import com.coinexchange.wallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class FundsSeeder implements CommandLineRunner {

    private static final BigDecimal SEED_KRW = new BigDecimal("100000000000");
    private static final long SEED_COIN = 100_000_000L;
    private static final Long SEED_COIN_ID = 1L;
    private static final int LOOKUP_MAX_ATTEMPTS = 10;
    private static final long LOOKUP_RETRY_DELAY_MS = 1000L;

    private final WalletRepository walletRepository;
    private final CoinWalletRepository coinWalletRepository;

    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;

    @Override
    @Transactional
    public void run(String... args) {
        if (walletRepository.count() > 0) {
            log.info("funds 시드 데이터 이미 존재 - 스킵");
            return;
        }

        RestClient userClient = RestClient.create(userServiceBaseUrl);
        Long buyerId = lookupUserId(userClient, "buyer@test.com");
        Long sellerId = lookupUserId(userClient, "seller@test.com");

        seedWallet(buyerId);
        seedWallet(sellerId);
        seedCoinWallet(buyerId, SEED_COIN_ID);
        seedCoinWallet(sellerId, SEED_COIN_ID);

        log.info("funds 시드 완료: buyer={}, seller={}, coinId={}", buyerId, sellerId, SEED_COIN_ID);
    }

    private Long lookupUserId(RestClient client, String email) {
        for (int attempt = 1; attempt <= LOOKUP_MAX_ATTEMPTS; attempt++) {
            try {
                UserLookupResponse response = client.get()
                        .uri("/internal/users/by-email/{email}", email)
                        .retrieve()
                        .body(UserLookupResponse.class);
                if (response != null && response.id() != null) {
                    return response.id();
                }
            } catch (Exception e) {
                log.warn("user-service 조회 실패 (attempt {}/{}): {}", attempt, LOOKUP_MAX_ATTEMPTS, e.getMessage());
            }
            try {
                Thread.sleep(LOOKUP_RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("user-service 조회 중 인터럽트", ie);
            }
        }
        throw new IllegalStateException("user-service에서 " + email + " 조회 실패");
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

    private record UserLookupResponse(Long id, String email) {
    }
}
