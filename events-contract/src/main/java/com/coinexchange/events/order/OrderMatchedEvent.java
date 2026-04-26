package com.coinexchange.events.order;

public record OrderMatchedEvent(
        Long tradeId,
        Long buyOrderId,
        Long sellOrderId,
        Long matchedAmount
) {
}
