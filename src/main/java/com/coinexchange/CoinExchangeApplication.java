package com.coinexchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoinExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinExchangeApplication.class, args);
    }

}
