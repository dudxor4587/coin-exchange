package com.coinexchange.trading.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 매칭이 체결됐음을 durable 로그(Kafka)에 남기는 이벤트.
 * hot path에서는 정산(settle)까지만 동기로 끝내고,
 * Trade INSERT / Order fill / 알림은 컨슈머가 projection한다.
 * eventId는 재소비 시 중복 반영을 막는 dedup 키다.
 */
public record TradeExecutedEvent(
        UUID eventId,
        Long buyOrderId,
        Long sellOrderId,
        Long buyerId,
        Long sellerId,
        Long coinId,
        Long matchedAmount,
        BigDecimal price
) {
    public TradeExecutedEvent(Long buyOrderId, Long sellOrderId, Long buyerId, Long sellerId,
                              Long coinId, Long matchedAmount, BigDecimal price) {
        this(UUID.randomUUID(), buyOrderId, sellOrderId, buyerId, sellerId, coinId, matchedAmount, price);
    }
}
