package com.coinexchange.events.withdraw;

public record WithdrawRejectedEvent(
        Long userId,
        String reason,
        Long withdrawId
) {
}
