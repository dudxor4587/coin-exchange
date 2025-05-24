package com.coinexchange.withdraw.event;

public record WithdrawRejectedEvent(
        Long userId,
        String reason,
        Long withdrawId
) {
}
