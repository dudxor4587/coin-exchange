package com.coinexchange.events.order;

import java.math.BigDecimal;

public record SellOrderFilledEvent(
        Long sellOrderId,
        Long userId,
        BigDecimal price
) {
}
