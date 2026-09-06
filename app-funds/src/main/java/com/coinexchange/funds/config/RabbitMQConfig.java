package com.coinexchange.funds.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.coinexchange.common.config.RabbitMQChannels.*;

/**
 * funds가 발행하는 알림 계열 채널만 선언한다.
 * 주문/체결 흐름은 5단계에서 동기 RPC로 되돌렸고, 주문 durable 로그는 Kafka가 맡는다.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue notificationRequestedQueue() {
        return new Queue(NOTIFICATION_REQUESTED_QUEUE);
    }

    @Bean
    public TopicExchange notificationRequestedExchange() {
        return new TopicExchange(NOTIFICATION_REQUESTED_EXCHANGE);
    }

    @Bean
    public Binding notificationRequestedBinding(Queue notificationRequestedQueue,
                                                TopicExchange notificationRequestedExchange) {
        return BindingBuilder.bind(notificationRequestedQueue)
                .to(notificationRequestedExchange)
                .with(NOTIFICATION_REQUESTED_ROUTING_KEY);
    }

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
        return BindingBuilder.bind(depositRejectQueue).to(depositRejectExchange).with(DEPOSIT_REJECT_ROUTING_KEY);
    }

    @Bean
    public Queue withdrawRejectQueue() {
        return new Queue(WITHDRAW_REJECT_QUEUE);
    }

    @Bean
    public TopicExchange withdrawRejectExchange() {
        return new TopicExchange(WITHDRAW_REJECT_EXCHANGE);
    }

    @Bean
    public Binding withdrawRejectBinding(Queue withdrawRejectQueue, TopicExchange withdrawRejectExchange) {
        return BindingBuilder.bind(withdrawRejectQueue).to(withdrawRejectExchange).with(WITHDRAW_REJECT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
