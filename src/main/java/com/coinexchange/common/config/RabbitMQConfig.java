package com.coinexchange.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 입금 요청 승인
    public static final String DEPOSIT_APPROVE_QUEUE = "wallet.deposit.approve.queue";
    public static final String DEPOSIT_APPROVE_EXCHANGE = "deposit.approve.exchange";
    public static final String DEPOSIT_APPROVE_ROUTING_KEY = "deposit.approved";

    // 입금 요청 거절
    public static final String DEPOSIT_REJECT_QUEUE = "wallet.deposit.reject.queue";
    public static final String DEPOSIT_REJECT_EXCHANGE = "deposit.reject.exchange";
    public static final String DEPOSIT_REJECT_ROUTING_KEY = "deposit.rejected";

    // 입금 요청 승인 큐
    @Bean
    public Queue depositQueue() {
        return new Queue(DEPOSIT_APPROVE_QUEUE);
    }

    @Bean
    public TopicExchange depositExchange() {
        return new TopicExchange(DEPOSIT_APPROVE_EXCHANGE);
    }

    @Bean
    public Binding depositBinding(Queue depositQueue, TopicExchange depositExchange) {
        return BindingBuilder
                .bind(depositQueue)
                .to(depositExchange)
                .with(DEPOSIT_APPROVE_ROUTING_KEY);
    }

    // 입금 요청 거절 큐
    @Bean
    public Queue depositRejectQueue() {
        return new Queue(DEPOSIT_REJECT_QUEUE);
    }

    @Bean
    public TopicExchange depositRejectExchange() {
        return new TopicExchange(DEPOSIT_REJECT_EXCHANGE);
    }

    @Bean
    public Binding depositRejectBinding(Queue depositRejectQueue, TopicExchange depositRejectExchange) {
        return BindingBuilder
                .bind(depositRejectQueue)
                .to(depositRejectExchange)
                .with(DEPOSIT_REJECT_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
