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

    public static final String DEPOSIT_QUEUE = "wallet.deposit.queue";
    public static final String DEPOSIT_EXCHANGE = "deposit.exchange";
    public static final String DEPOSIT_ROUTING_KEY = "deposit.approved";

    @Bean
    public Queue depositQueue() {
        return new Queue(DEPOSIT_QUEUE);
    }

    @Bean
    public TopicExchange depositExchange() {
        return new TopicExchange(DEPOSIT_EXCHANGE);
    }

    @Bean
    public Binding depositBinding(Queue depositQueue, TopicExchange depositExchange) {
        return BindingBuilder
                .bind(depositQueue)
                .to(depositExchange)
                .with(DEPOSIT_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
