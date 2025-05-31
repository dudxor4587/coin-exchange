package com.coinexchange.order.event;

public record TradeRollbackEvent(
        Long tradeId,
        String reason
) {
}
