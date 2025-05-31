package com.coinexchange.order.event;

public record BuyOrderFilledEvent(
        Long buyOrderId,
        Long userId,
        Long coinId,
        Long amount
) {
}
