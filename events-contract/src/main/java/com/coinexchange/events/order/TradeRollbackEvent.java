package com.coinexchange.events.order;

public record TradeRollbackEvent(
        Long tradeId,
        String reason
) {
}
