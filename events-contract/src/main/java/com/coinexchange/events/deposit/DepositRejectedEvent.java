package com.coinexchange.events.deposit;

public record DepositRejectedEvent(
        Long userId,
        String reason
) {
}
