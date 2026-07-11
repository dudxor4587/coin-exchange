package com.coinexchange.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.coinexchange.trading",
        "com.coinexchange.matching",
        "com.coinexchange.coin",
        "com.coinexchange.common"
})
@EntityScan(basePackages = {
        "com.coinexchange.coin.domain",
        "com.coinexchange.common.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.coinexchange.coin.domain.repository"
})
@EnableScheduling
public class TradingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingServiceApplication.class, args);
    }
}
