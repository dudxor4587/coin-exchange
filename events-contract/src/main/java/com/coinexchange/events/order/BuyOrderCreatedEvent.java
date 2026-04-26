package com.coinexchange.events.order;

import java.math.BigDecimal;

public record BuyOrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal lockedFunds,
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
