package com.coinexchange.events.order;

/**
 * 주문 durable 로그(Kafka)의 wire 계약.
 * producer(trading)와 consumer(projection)가 다른 서비스로 갈렸으므로,
 * 토픽 이름과 eventType 헤더 규약을 공유 계약으로 여기 둔다.
 */
public final class OrderLogChannel {

    public static final String TOPIC = "order.log";

    public static final String HEADER_EVENT_TYPE = "eventType";
    public static final String TYPE_ORDER_PLACED = "ORDER_PLACED";
    public static final String TYPE_TRADE_EXECUTED = "TRADE_EXECUTED";

    private OrderLogChannel() {
    }
}
