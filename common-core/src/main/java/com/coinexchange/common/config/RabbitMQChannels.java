package com.coinexchange.common.config;

public final class RabbitMQChannels {

    private RabbitMQChannels() {
    }

    public static final String DEPOSIT_APPROVE_QUEUE = "wallet.deposit.approve.queue";
    public static final String DEPOSIT_APPROVE_EXCHANGE = "deposit.approve.exchange";
    public static final String DEPOSIT_APPROVE_ROUTING_KEY = "deposit.approved";

    public static final String DEPOSIT_REJECT_QUEUE = "wallet.deposit.reject.queue";
    public static final String DEPOSIT_REJECT_EXCHANGE = "deposit.reject.exchange";
    public static final String DEPOSIT_REJECT_ROUTING_KEY = "deposit.rejected";

    public static final String WITHDRAW_APPROVE_QUEUE = "wallet.withdraw.approve.queue";
    public static final String WITHDRAW_APPROVE_EXCHANGE = "withdraw.approve.exchange";
    public static final String WITHDRAW_APPROVE_ROUTING_KEY = "withdraw.approved";

    public static final String WITHDRAW_REJECT_QUEUE = "wallet.withdraw.reject.queue";
    public static final String WITHDRAW_REJECT_EXCHANGE = "withdraw.reject.exchange";
    public static final String WITHDRAW_REJECT_ROUTING_KEY = "withdraw.rejected";

    public static final String WITHDRAW_FAILURE_QUEUE = "withdraw.failure.queue";
    public static final String WITHDRAW_FAILURE_EXCHANGE = "withdraw.failure.exchange";
    public static final String WITHDRAW_FAILURE_ROUTING_KEY = "withdraw.failed";

    public static final String BUY_ORDER_CREATED_QUEUE = "order.buy.created.queue";
    public static final String BUY_ORDER_CREATED_EXCHANGE = "order.buy.created.exchange";
    public static final String BUY_ORDER_CREATED_ROUTING_KEY = "order.buy.created";

    public static final String SELL_ORDER_CREATED_QUEUE = "order.sell.created.queue";
    public static final String SELL_ORDER_CREATED_EXCHANGE = "order.sell.created.exchange";
    public static final String SELL_ORDER_CREATED_ROUTING_KEY = "order.sell.created";

    public static final String ORDER_PROCESSING_FAILED_QUEUE = "order.processing.failed.queue";
    public static final String ORDER_PROCESSING_FAILED_EXCHANGE = "order.processing.failed.exchange";
    public static final String ORDER_PROCESSING_FAILED_ROUTING_KEY = "order.processing.failed";

    public static final String BUY_ORDER_READY_QUEUE = "order.buy.ready.queue";
    public static final String BUY_ORDER_READY_EXCHANGE = "order.buy.ready.exchange";
    public static final String BUY_ORDER_READY_ROUTING_KEY = "order.buy.ready";

    public static final String SELL_ORDER_READY_QUEUE = "order.sell.ready.queue";
    public static final String SELL_ORDER_READY_EXCHANGE = "order.sell.ready.exchange";
    public static final String SELL_ORDER_READY_ROUTING_KEY = "order.sell.ready";

    public static final String ORDER_MATCHED_QUEUE = "order.matched.queue";
    public static final String ORDER_MATCHED_EXCHANGE = "order.matched.exchange";
    public static final String ORDER_MATCHED_ROUTING_KEY = "order.matched";

    public static final String BUY_ORDER_FILLED_QUEUE = "order.buy.filled.queue";
    public static final String BUY_ORDER_FILLED_EXCHANGE = "order.buy.filled.exchange";
    public static final String BUY_ORDER_FILLED_ROUTING_KEY = "order.buy.filled";

    public static final String SELL_ORDER_FILLED_QUEUE = "order.sell.filled.queue";
    public static final String SELL_ORDER_FILLED_EXCHANGE = "order.sell.filled.exchange";
    public static final String SELL_ORDER_FILLED_ROUTING_KEY = "order.sell.filled";

    public static final String BUY_ORDER_COMPLETED_QUEUE = "order.buy.completed.queue";
    public static final String BUY_ORDER_COMPLETED_EXCHANGE = "order.buy.completed.exchange";
    public static final String BUY_ORDER_COMPLETED_ROUTING_KEY = "order.buy.completed";

    public static final String SELL_ORDER_COMPLETED_QUEUE = "order.sell.completed.queue";
    public static final String SELL_ORDER_COMPLETED_EXCHANGE = "order.sell.completed.exchange";
    public static final String SELL_ORDER_COMPLETED_ROUTING_KEY = "order.sell.completed";

    public static final String TRADE_CREATED_QUEUE = "trade.created.queue";
    public static final String TRADE_CREATED_EXCHANGE = "trade.created.exchange";
    public static final String TRADE_CREATED_ROUTING_KEY = "trade.created";

    public static final String ORDER_BOOK_ROLLBACK_QUEUE = "order.book.rollback.queue";
    public static final String ORDER_BOOK_ROLLBACK_EXCHANGE = "order.book.rollback.exchange";
    public static final String ORDER_BOOK_ROLLBACK_ROUTING_KEY = "order.book.rollback";

    public static final String TRADE_ROLLBACK_QUEUE = "trade.rollback.queue";
    public static final String TRADE_ROLLBACK_EXCHANGE = "trade.rollback.exchange";
    public static final String TRADE_ROLLBACK_ROUTING_KEY = "trade.rollback";
}
