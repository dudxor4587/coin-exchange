package com.coinexchange.order.event;

public record OrderMatchedEvent(
        Long tradeId,
        Long buyOrderId,
        Long sellOrderId,
        Long matchedAmount
) {
}
