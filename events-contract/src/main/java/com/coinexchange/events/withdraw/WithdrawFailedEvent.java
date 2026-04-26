package com.coinexchange.events.withdraw;

public record WithdrawFailedEvent(
        String reason,
        Long withdrawId
) {
}
