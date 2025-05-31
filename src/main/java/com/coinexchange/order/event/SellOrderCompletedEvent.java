package com.coinexchange.order.event;

public record SellOrderCompletedEvent(
        Long orderId,
        Long userId,
        Long coinId,
        Long amount
) {
}
