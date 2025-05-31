package com.coinexchange.order.event;

import java.math.BigDecimal;

public record BuyOrderReadyEvent(
        Long orderId,
        Long userId,
        Long coinId,
        BigDecimal lockedFunds,
        BigDecimal price,
        Long amount
) {
}
