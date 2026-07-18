package com.coinexchange.trading.infra;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka 헬스 지표. Spring Boot는 db·redis·rabbit은 자동으로 만들어주지만 Kafka는 안 만들어준다.
 * trading은 주문을 durable 로그(Kafka)에 append하므로 Kafka가 하드 의존성이다 — 죽으면 not-ready여야 한다.
 * AdminClient로 브로커에 describeCluster를 2초 타임아웃으로 던져 도달 가능한지만 확인한다.
 */
@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            String clusterId = client.describeCluster(new DescribeClusterOptions().timeoutMs(2000))
                    .clusterId().get(2, TimeUnit.SECONDS);
            return Health.up().withDetail("clusterId", clusterId).build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
