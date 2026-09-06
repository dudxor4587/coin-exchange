package com.coinexchange.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주문 hot path 서비스 — 무상태(Redis + Kafka + funds RPC). DB를 쓰지 않는다.
 * JPA는 classpath에도 없다(BaseTimeEntity를 common-jpa로 떼어내 의존이 끊겼다).
 * 주문의 DB 반영과 coin 시드는 projection 서비스가 맡는다.
 */
@SpringBootApplication(scanBasePackages = {
        "com.coinexchange.trading",
        "com.coinexchange.matching",
        "com.coinexchange.common"
})
@EnableScheduling
public class TradingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingServiceApplication.class, args);
    }
}
