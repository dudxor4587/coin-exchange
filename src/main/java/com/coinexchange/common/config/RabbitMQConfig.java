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

    // 출금 요청 승인
    public static final String WITHDRAW_APPROVE_QUEUE = "wallet.withdraw.approve.queue";
    public static final String WITHDRAW_APPROVE_EXCHANGE = "withdraw.approve.exchange";
    public static final String WITHDRAW_APPROVE_ROUTING_KEY = "withdraw.approved";

    // 출금 요청 거절
    public static final String WITHDRAW_REJECT_QUEUE = "wallet.withdraw.reject.queue";
    public static final String WITHDRAW_REJECT_EXCHANGE = "withdraw.reject.exchange";
    public static final String WITHDRAW_REJECT_ROUTING_KEY = "withdraw.rejected";

    // 출금 실패
    public static final String WITHDRAW_FAILURE_QUEUE = "withdraw.failure.queue";
    public static final String WITHDRAW_FAILURE_EXCHANGE = "withdraw.failure.exchange";
    public static final String WITHDRAW_FAILURE_ROUTING_KEY = "withdraw.failed";

    // 매수 정보 저장 성공
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_CREATED_EXCHANGE = "order.created.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    // 주문 처리 실패
    public static final String ORDER_PROCESSING_FAILED_QUEUE = "order.processing.failed.queue";
    public static final String ORDER_PROCESSING_FAILED_EXCHANGE = "order.processing.failed.exchange";
    public static final String ORDER_PROCESSING_FAILED_ROUTING_KEY = "order.processing.failed";

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

    // 출금 요청 승인 큐
    @Bean
    public Queue withdrawQueue() {
        return new Queue(WITHDRAW_APPROVE_QUEUE);
    }

    @Bean
    public TopicExchange withdrawExchange() {
        return new TopicExchange(WITHDRAW_APPROVE_EXCHANGE);
    }

    @Bean
    public Binding withdrawBinding(Queue withdrawQueue, TopicExchange withdrawExchange) {
        return BindingBuilder
                .bind(withdrawQueue)
                .to(withdrawExchange)
                .with(WITHDRAW_APPROVE_ROUTING_KEY);
    }

    // 출금 요청 거절 큐
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
        return BindingBuilder
                .bind(withdrawRejectQueue)
                .to(withdrawRejectExchange)
                .with(WITHDRAW_REJECT_ROUTING_KEY);
    }

    // 출금 실패 큐
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
        return BindingBuilder
                .bind(withdrawFailureQueue)
                .to(withdrawFailureExchange)
                .with(WITHDRAW_FAILURE_ROUTING_KEY);
    }

    // 매수 정보 저장 큐
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE);
    }

    @Bean
    public TopicExchange orderCreatedExchange() {
        return new TopicExchange(ORDER_CREATED_EXCHANGE);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderCreatedExchange) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderCreatedExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    // 주문 처리 실패 큐
    @Bean
    public Queue orderProcessingFailedQueue() {
        return new Queue(ORDER_PROCESSING_FAILED_QUEUE);
    }

    @Bean
    public TopicExchange orderProcessingFailedExchange() {
        return new TopicExchange(ORDER_PROCESSING_FAILED_EXCHANGE);
    }

    @Bean
    public Binding orderProcessingFailedBinding(Queue orderProcessingFailedQueue, TopicExchange orderProcessingFailedExchange) {
        return BindingBuilder
                .bind(orderProcessingFailedQueue)
                .to(orderProcessingFailedExchange)
                .with(ORDER_PROCESSING_FAILED_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
