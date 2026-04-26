package com.coinexchange.events.order;

public record BuyOrderFilledEvent(
        Long buyOrderId,
        Long userId,
        Long coinId,
        Long amount
) {
}
