package com.coinexchange.order.event;

import java.math.BigDecimal;

public record SellOrderFilledEvent(
        Long sellOrderId,
        Long userId,
        BigDecimal price
) {
}
