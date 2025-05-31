package com.coinexchange.trade.event;

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
