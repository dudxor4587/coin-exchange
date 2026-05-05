package com.coinexchange.trading.seed;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.coin.domain.repository.CoinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class CoinSeeder implements CommandLineRunner {

    private final CoinRepository coinRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (coinRepository.count() > 0) {
            log.info("coin 시드 데이터 이미 존재 - 스킵");
            return;
        }

        Coin coin = coinRepository.save(Coin.builder()
                .symbol(Coin.Symbol.BTC)
                .name("Bitcoin")
                .build());

        log.info("coin 시드 완료: coinId={}", coin.getId());
    }
}
