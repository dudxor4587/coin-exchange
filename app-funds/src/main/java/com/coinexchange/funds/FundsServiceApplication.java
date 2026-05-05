package com.coinexchange.funds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.coinexchange.funds",
        "com.coinexchange.wallet",
        "com.coinexchange.deposit",
        "com.coinexchange.withdraw",
        "com.coinexchange.common"
})
@EntityScan(basePackages = {
        "com.coinexchange.wallet.domain",
        "com.coinexchange.deposit.domain",
        "com.coinexchange.withdraw.domain",
        "com.coinexchange.common.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.coinexchange.wallet.domain.repository",
        "com.coinexchange.deposit.domain.repository",
        "com.coinexchange.withdraw.domain.repository"
})
public class FundsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundsServiceApplication.class, args);
    }
}
