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
    public static final String BUY_ORDER_CREATED_QUEUE = "order.buy.created.queue";
    public static final String BUY_ORDER_CREATED_EXCHANGE = "order.buy.created.exchange";
    public static final String BUY_ORDER_CREATED_ROUTING_KEY = "order.buy.created";

    // 매도 정보 저장 성공
    public static final String SELL_ORDER_CREATED_QUEUE = "order.sell.created.queue";
    public static final String SELL_ORDER_CREATED_EXCHANGE = "order.sell.created.exchange";
    public static final String SELL_ORDER_CREATED_ROUTING_KEY = "order.sell.created";

    // 주문 처리 실패
    public static final String ORDER_PROCESSING_FAILED_QUEUE = "order.processing.failed.queue";
    public static final String ORDER_PROCESSING_FAILED_EXCHANGE = "order.processing.failed.exchange";
    public static final String ORDER_PROCESSING_FAILED_ROUTING_KEY = "order.processing.failed";

    // 매수 주문 매칭 준비 완료
    public static final String BUY_ORDER_READY_QUEUE = "order.buy.ready.queue";
    public static final String BUY_ORDER_READY_EXCHANGE = "order.buy.ready.exchange";
    public static final String BUY_ORDER_READY_ROUTING_KEY = "order.buy.ready";

    // 매도 주문 매칭 준비 완료
    public static final String SELL_ORDER_READY_QUEUE = "order.sell.ready.queue";
    public static final String SELL_ORDER_READY_EXCHANGE = "order.sell.ready.exchange";
    public static final String SELL_ORDER_READY_ROUTING_KEY = "order.sell.ready";

    // 매칭 완료
    public static final String ORDER_MATCHED_QUEUE = "order.matched.queue";
    public static final String ORDER_MATCHED_EXCHANGE = "order.matched.exchange";
    public static final String ORDER_MATCHED_ROUTING_KEY = "order.matched";

    // 매수 주문 체결
    public static final String BUY_ORDER_FILLED_QUEUE = "order.buy.filled.queue";
    public static final String BUY_ORDER_FILLED_EXCHANGE = "order.buy.filled.exchange";
    public static final String BUY_ORDER_FILLED_ROUTING_KEY = "order.buy.filled";

    // 매도 주문 체결
    public static final String SELL_ORDER_FILLED_QUEUE = "order.sell.filled.queue";
    public static final String SELL_ORDER_FILLED_EXCHANGE = "order.sell.filled.exchange";
    public static final String SELL_ORDER_FILLED_ROUTING_KEY = "order.sell.filled";


    // 매수 주문 모두 매칭 완료
    public static final String BUY_ORDER_COMPLETED_QUEUE = "order.buy.completed.queue";
    public static final String BUY_ORDER_COMPLETED_EXCHANGE = "order.buy.completed.exchange";
    public static final String BUY_ORDER_COMPLETED_ROUTING_KEY = "order.buy.completed";

    // 매도 주문 모두 매칭 완료
    public static final String SELL_ORDER_COMPLETED_QUEUE = "order.sell.completed.queue";
    public static final String SELL_ORDER_COMPLETED_EXCHANGE = "order.sell.completed.exchange";
    public static final String SELL_ORDER_COMPLETED_ROUTING_KEY = "order.sell.completed";

    // 체결 정보 저장 큐
    public static final String TRADE_CREATED_QUEUE = "trade.created.queue";
    public static final String TRADE_CREATED_EXCHANGE = "trade.created.exchange";
    public static final String TRADE_CREATED_ROUTING_KEY = "trade.created";

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
        return new Queue(BUY_ORDER_CREATED_QUEUE);
    }

    @Bean
    public TopicExchange orderCreatedExchange() {
        return new TopicExchange(BUY_ORDER_CREATED_EXCHANGE);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderCreatedExchange) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderCreatedExchange)
                .with(BUY_ORDER_CREATED_ROUTING_KEY);
    }

    // 매도 정보 저장 큐
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
        return BindingBuilder
                .bind(sellOrderCreatedQueue)
                .to(sellOrderCreatedExchange)
                .with(SELL_ORDER_CREATED_ROUTING_KEY);
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

    // 매수 주문 매칭 준비 완료 큐
    @Bean
    public Queue buyOrderReadyQueue() {
        return new Queue(BUY_ORDER_READY_QUEUE);
    }

    @Bean
    public TopicExchange buyOrderReadyExchange() {
        return new TopicExchange(BUY_ORDER_READY_EXCHANGE);
    }

    @Bean
    public Binding buyOrderReadyBinding(Queue buyOrderReadyQueue, TopicExchange buyOrderReadyExchange) {
        return BindingBuilder
                .bind(buyOrderReadyQueue)
                .to(buyOrderReadyExchange)
                .with(BUY_ORDER_READY_ROUTING_KEY);
    }

    // 매도 주문 매칭 준비 완료 큐
    @Bean
    public Queue sellOrderReadyQueue() {
        return new Queue(SELL_ORDER_READY_QUEUE);
    }

    @Bean
    public TopicExchange sellOrderReadyExchange() {
        return new TopicExchange(SELL_ORDER_READY_EXCHANGE);
    }

    @Bean
    public Binding sellOrderReadyBinding(Queue sellOrderReadyQueue, TopicExchange sellOrderReadyExchange) {
        return BindingBuilder
                .bind(sellOrderReadyQueue)
                .to(sellOrderReadyExchange)
                .with(SELL_ORDER_READY_ROUTING_KEY);
    }

    // 매칭 완료 큐
    @Bean
    public Queue orderMatchedQueue() {
        return new Queue(ORDER_MATCHED_QUEUE);
    }

    @Bean
    public TopicExchange orderMatchedExchange() {
        return new TopicExchange(ORDER_MATCHED_EXCHANGE);
    }

    @Bean
    public Binding orderMatchedBinding(Queue orderMatchedQueue, TopicExchange orderMatchedExchange) {
        return BindingBuilder
                .bind(orderMatchedQueue)
                .to(orderMatchedExchange)
                .with(ORDER_MATCHED_ROUTING_KEY);
    }

    // 매수 주문 체결 큐
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
        return BindingBuilder
                .bind(buyOrderFilledQueue)
                .to(buyOrderFilledExchange)
                .with(BUY_ORDER_FILLED_ROUTING_KEY);
    }

    // 매도 주문 체결 큐
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
        return BindingBuilder
                .bind(sellOrderFilledQueue)
                .to(sellOrderFilledExchange)
                .with(SELL_ORDER_FILLED_ROUTING_KEY);
    }

    // 매수 주문 모두 매칭 완료 큐
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
        return BindingBuilder
                .bind(buyOrderCompletedQueue)
                .to(buyOrderCompletedExchange)
                .with(BUY_ORDER_COMPLETED_ROUTING_KEY);
    }

    // 매도 주문 모두 매칭 완료 큐
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
        return BindingBuilder
                .bind(sellOrderCompletedQueue)
                .to(sellOrderCompletedExchange)
                .with(SELL_ORDER_COMPLETED_ROUTING_KEY);
    }

    // 체결 정보 저장 큐
    @Bean
    public Queue tradeCreatedQueue() {
        return new Queue(TRADE_CREATED_QUEUE);
    }

    @Bean
    public TopicExchange tradeCreatedExchange() {
        return new TopicExchange(TRADE_CREATED_EXCHANGE);
    }

    @Bean
    public Binding tradeCreatedBinding(Queue tradeCreatedQueue, TopicExchange tradeCreatedExchange) {
        return BindingBuilder
                .bind(tradeCreatedQueue)
                .to(tradeCreatedExchange)
                .with(TRADE_CREATED_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
