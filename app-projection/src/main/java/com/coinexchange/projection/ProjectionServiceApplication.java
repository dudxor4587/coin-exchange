package com.coinexchange.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 주문 durable 로그(Kafka)를 읽어 DB에 projection하는 컨슈머 서비스.
 * trading(hot path)에서 컨슈머를 들어내 자원 경쟁을 없애기 위해 분리됐다.
 * 매칭엔진(Redis)은 모르고, 로그 소비와 DB 반영만 한다.
 */
@SpringBootApplication(scanBasePackages = {
        "com.coinexchange.projection",
        "com.coinexchange.order",
        "com.coinexchange.trade",
        "com.coinexchange.common"
})
@EntityScan(basePackages = {
        "com.coinexchange.order.domain",
        "com.coinexchange.trade.domain",
        "com.coinexchange.common.domain",
        "com.coinexchange.projection.infra.projection"
})
@EnableJpaRepositories(basePackages = {
        "com.coinexchange.order.domain.repository",
        "com.coinexchange.trade.domain.repository",
        "com.coinexchange.projection.infra.projection"
})
public class ProjectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectionServiceApplication.class, args);
    }
}
