package com.coinexchange.events.withdraw;

import java.util.UUID;

public record WithdrawRejectedEvent(
        UUID eventId,
        Long userId,
        String reason,
        Long withdrawId
) {
    public WithdrawRejectedEvent(Long userId, String reason, Long withdrawId) {
        this(UUID.randomUUID(), userId, reason, withdrawId);
    }
}
