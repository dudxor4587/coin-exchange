package com.coinexchange.events.order;

import java.math.BigDecimal;

public record SellOrderCreatedEvent(
        Long orderId,
        Long userId,
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
