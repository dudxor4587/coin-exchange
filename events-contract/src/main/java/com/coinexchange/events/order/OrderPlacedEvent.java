package com.coinexchange.events.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 주문이 접수됐음을 durable 로그(Kafka)에 남기는 이벤트.
 * 매칭엔진(producer)과 projection(consumer)이 공유하는 wire 계약이라,
 * 도메인 타입에 의존하지 않도록 side는 "BUY"/"SELL" 문자열로 둔다.
 * eventId는 at-least-once 재소비 시 중복 반영을 막는 dedup 키다.
 */
public record OrderPlacedEvent(
        UUID eventId,
        Long orderId,
        Long coinId,
        BigDecimal price,
        Long amount,
        Long userId,
        String side,
        BigDecimal lockedFunds
) {
    public OrderPlacedEvent(Long orderId, Long coinId, BigDecimal price, Long amount,
                            Long userId, String side, BigDecimal lockedFunds) {
        this(UUID.randomUUID(), orderId, coinId, price, amount, userId, side, lockedFunds);
    }
}
