package com.coinexchange.order.event;

import java.math.BigDecimal;

public record SellOrderReadyEvent(
        Long orderId,
        Long userId,
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
