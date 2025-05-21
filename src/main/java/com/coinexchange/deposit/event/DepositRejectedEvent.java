package com.coinexchange.deposit.event;

public record DepositRejectedEvent(
        Long userId,
        String reason
) {
}
