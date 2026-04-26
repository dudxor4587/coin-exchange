package com.coinexchange.events.order;

public record SellOrderCompletedEvent(
        Long orderId,
        Long userId,
        Long coinId,
        Long amount
) {
}
