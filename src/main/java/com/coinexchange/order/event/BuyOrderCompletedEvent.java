package com.coinexchange.order.event;

public record BuyOrderCompletedEvent(
        Long orderId,
        Long userId,
        Long coinId,
        Long amount
) {
}
