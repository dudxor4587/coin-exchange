package com.coinexchange.order.event;

public record OrderMatchedEvent(
        Long buyOrderId,
        Long sellOrderId,
        Long matchedAmount
) {
}
