package com.coinexchange.events.deposit;

import java.util.UUID;

public record DepositRejectedEvent(
        UUID eventId,
        Long userId,
        String reason
) {
    public DepositRejectedEvent(Long userId, String reason) {
        this(UUID.randomUUID(), userId, reason);
    }
}
