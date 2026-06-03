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

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue buyOrderCreatedQueue() {
        return new Queue(BUY_ORDER_CREATED_QUEUE);
    }

    @Bean
    public TopicExchange buyOrderCreatedExchange() {
        return new TopicExchange(BUY_ORDER_CREATED_EXCHANGE);
    }

    @Bean
    public Binding buyOrderCreatedBinding(Queue buyOrderCreatedQueue, TopicExchange buyOrderCreatedExchange) {
        return BindingBuilder.bind(buyOrderCreatedQueue).to(buyOrderCreatedExchange).with(BUY_ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Queue sellOrderCreatedQueue() {
        return new Queue(SELL_ORDER_CREATED_QUEUE);
    }

    @Bean
    public TopicExchange sellOrderCreatedExchange() {
        return new TopicExchange(SELL_ORDER_CREATED_EXCHANGE);
    }

    @Bean
    public Binding sellOrderCreatedBinding(Queue sellOrderCreatedQueue, TopicExchange sellOrderCreatedExchange) {
        return BindingBuilder.bind(sellOrderCreatedQueue).to(sellOrderCreatedExchange).with(SELL_ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Queue buyOrderFilledQueue() {
        return new Queue(BUY_ORDER_FILLED_QUEUE);
    }

    @Bean
    public TopicExchange buyOrderFilledExchange() {
        return new TopicExchange(BUY_ORDER_FILLED_EXCHANGE);
    }

    @Bean
    public Binding buyOrderFilledBinding(Queue buyOrderFilledQueue, TopicExchange buyOrderFilledExchange) {
        return BindingBuilder.bind(buyOrderFilledQueue).to(buyOrderFilledExchange).with(BUY_ORDER_FILLED_ROUTING_KEY);
    }

    @Bean
    public Queue sellOrderFilledQueue() {
        return new Queue(SELL_ORDER_FILLED_QUEUE);
    }

    @Bean
    public TopicExchange sellOrderFilledExchange() {
        return new TopicExchange(SELL_ORDER_FILLED_EXCHANGE);
    }

    @Bean
    public Binding sellOrderFilledBinding(Queue sellOrderFilledQueue, TopicExchange sellOrderFilledExchange) {
        return BindingBuilder.bind(sellOrderFilledQueue).to(sellOrderFilledExchange).with(SELL_ORDER_FILLED_ROUTING_KEY);
    }

    @Bean
    public Queue buyOrderCompletedQueue() {
        return new Queue(BUY_ORDER_COMPLETED_QUEUE);
    }

    @Bean
    public TopicExchange buyOrderCompletedExchange() {
        return new TopicExchange(BUY_ORDER_COMPLETED_EXCHANGE);
    }

    @Bean
    public Binding buyOrderCompletedBinding(Queue buyOrderCompletedQueue, TopicExchange buyOrderCompletedExchange) {
        return BindingBuilder.bind(buyOrderCompletedQueue).to(buyOrderCompletedExchange).with(BUY_ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Queue sellOrderCompletedQueue() {
        return new Queue(SELL_ORDER_COMPLETED_QUEUE);
    }

    @Bean
    public TopicExchange sellOrderCompletedExchange() {
        return new TopicExchange(SELL_ORDER_COMPLETED_EXCHANGE);
    }

    @Bean
    public Binding sellOrderCompletedBinding(Queue sellOrderCompletedQueue, TopicExchange sellOrderCompletedExchange) {
        return BindingBuilder.bind(sellOrderCompletedQueue).to(sellOrderCompletedExchange).with(SELL_ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Queue depositApproveQueue() {
        return new Queue(DEPOSIT_APPROVE_QUEUE);
    }

    @Bean
    public TopicExchange depositApproveExchange() {
        return new TopicExchange(DEPOSIT_APPROVE_EXCHANGE);
    }

    @Bean
    public Binding depositApproveBinding(Queue depositApproveQueue, TopicExchange depositApproveExchange) {
        return BindingBuilder.bind(depositApproveQueue).to(depositApproveExchange).with(DEPOSIT_APPROVE_ROUTING_KEY);
    }

    @Bean
    public Queue withdrawApproveQueue() {
        return new Queue(WITHDRAW_APPROVE_QUEUE);
    }

    @Bean
    public TopicExchange withdrawApproveExchange() {
        return new TopicExchange(WITHDRAW_APPROVE_EXCHANGE);
    }

    @Bean
    public Binding withdrawApproveBinding(Queue withdrawApproveQueue, TopicExchange withdrawApproveExchange) {
        return BindingBuilder.bind(withdrawApproveQueue).to(withdrawApproveExchange).with(WITHDRAW_APPROVE_ROUTING_KEY);
    }

    @Bean
    public Queue withdrawFailureQueue() {
        return new Queue(WITHDRAW_FAILURE_QUEUE);
    }

    @Bean
    public TopicExchange withdrawFailureExchange() {
        return new TopicExchange(WITHDRAW_FAILURE_EXCHANGE);
    }

    @Bean
    public Binding withdrawFailureBinding(Queue withdrawFailureQueue, TopicExchange withdrawFailureExchange) {
        return BindingBuilder.bind(withdrawFailureQueue).to(withdrawFailureExchange).with(WITHDRAW_FAILURE_ROUTING_KEY);
    }

    @Bean
    public TopicExchange buyOrderReadyExchange() {
        return new TopicExchange(BUY_ORDER_READY_EXCHANGE);
    }

    @Bean
    public TopicExchange sellOrderReadyExchange() {
        return new TopicExchange(SELL_ORDER_READY_EXCHANGE);
    }

    @Bean
    public TopicExchange depositRejectExchange() {
        return new TopicExchange(DEPOSIT_REJECT_EXCHANGE);
    }

    @Bean
    public TopicExchange withdrawRejectExchange() {
        return new TopicExchange(WITHDRAW_REJECT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderProcessingFailedExchange() {
        return new TopicExchange(ORDER_PROCESSING_FAILED_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationRequestedExchange() {
        return new TopicExchange(NOTIFICATION_REQUESTED_EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
