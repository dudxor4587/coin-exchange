package com.coinexchange.events.trade;

import java.math.BigDecimal;

public record TradeCreatedEvent(
        Long buyOrderId,
        Long sellOrderId,
        Long buyUserId,
        Long sellUserId,
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
