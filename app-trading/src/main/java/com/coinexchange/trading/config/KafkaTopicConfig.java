package com.coinexchange.trading.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_LOG_TOPIC = "order.log";

    /**
     * 주문 durable 로그는 파티션을 1개로 둔다.
     * 주문 접수(OrderPlaced)가 그 주문의 체결(TradeExecuted)보다 로그상 반드시 앞서야
     * 컨슈머의 fillOrder가 깨지지 않는다. 단일 파티션이면 전체 이벤트가 하나의 순서로
     * 쌓이고 하나의 컨슈머 스레드가 그 순서대로 처리한다.
     * 전체 순서를 보장하는 대가로 이 토픽은 병렬 확장하지 못한다 — durable + 순서의 비용이다.
     */
    @Bean
    public NewTopic orderLogTopic() {
        return TopicBuilder.name(ORDER_LOG_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
