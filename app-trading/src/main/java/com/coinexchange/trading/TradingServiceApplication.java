package com.coinexchange.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주문 hot path 서비스 — 무상태(Redis + Kafka + funds RPC). DB를 쓰지 않는다.
 * common-core를 통해 JPA가 classpath에 딸려오지만 사용하지 않으므로 auto-config를 제외한다.
 * 주문의 DB 반영과 coin 시드는 projection 서비스가 맡는다.
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.coinexchange.trading",
                "com.coinexchange.matching",
                "com.coinexchange.common"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        }
)
@EnableScheduling
public class TradingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingServiceApplication.class, args);
    }
}
