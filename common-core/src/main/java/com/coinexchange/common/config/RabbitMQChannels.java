package com.coinexchange.common.config;

/**
 * RabbitMQ 채널 상수.
 * 5단계에서 주문/체결 흐름을 동기 RPC로 되돌리면서, RabbitMQ에 남은 것은 알림 계열뿐이다.
 * 주문 durable 로그는 Kafka(OrderLogChannel)가 담당한다.
 */
public final class RabbitMQChannels {

    private RabbitMQChannels() {
    }

    public static final String DEPOSIT_REJECT_QUEUE = "wallet.deposit.reject.queue";
    public static final String DEPOSIT_REJECT_EXCHANGE = "deposit.reject.exchange";
    public static final String DEPOSIT_REJECT_ROUTING_KEY = "deposit.rejected";

    public static final String WITHDRAW_REJECT_QUEUE = "wallet.withdraw.reject.queue";
    public static final String WITHDRAW_REJECT_EXCHANGE = "withdraw.reject.exchange";
    public static final String WITHDRAW_REJECT_ROUTING_KEY = "withdraw.rejected";

    public static final String NOTIFICATION_REQUESTED_QUEUE = "notification.requested.queue";
    public static final String NOTIFICATION_REQUESTED_EXCHANGE = "notification.requested.exchange";
    public static final String NOTIFICATION_REQUESTED_ROUTING_KEY = "notification.requested";
}
