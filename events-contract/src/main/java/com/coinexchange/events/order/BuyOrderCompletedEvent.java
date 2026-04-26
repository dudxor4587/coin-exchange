package com.coinexchange.events.order;

public record BuyOrderCompletedEvent(
        Long orderId,
        Long userId,
        Long coinId,
        Long amount
) {
}
