package com.coinexchange.order.event;

public record OrderBookRollbackEvent(
        Long buyOrderId,
        Long sellOrderId,
        Long matchedAmount
) {
}
