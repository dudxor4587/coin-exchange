package com.coinexchange.projection.infra;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka 헬스 지표. Spring Boot는 Kafka health를 자동으로 만들어주지 않는다.
 * projection은 Kafka를 소비해 DB에 반영하므로 Kafka가 하드 의존성이다 — 죽으면 not-ready여야 한다.
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
