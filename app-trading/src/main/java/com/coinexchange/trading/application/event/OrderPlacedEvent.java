package com.coinexchange.trading.application.event;

import com.coinexchange.order.domain.Order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 주문이 접수(Redis OrderBook 등록)됐음을 durable 로그(Kafka)에 남기는 이벤트.
 * 컨슈머가 이걸 받아 Order를 DB에 projection한다.
 * eventId는 at-least-once 재소비 시 중복 반영을 막는 dedup 키다.
 */
public record OrderPlacedEvent(
        UUID eventId,
        Long orderId,
        Long coinId,
        BigDecimal price,
        Long amount,
        Long userId,
        Order.Type type,
        BigDecimal lockedFunds
) {
    public OrderPlacedEvent(Long orderId, Long coinId, BigDecimal price, Long amount,
                            Long userId, Order.Type type, BigDecimal lockedFunds) {
        this(UUID.randomUUID(), orderId, coinId, price, amount, userId, type, lockedFunds);
    }
}
