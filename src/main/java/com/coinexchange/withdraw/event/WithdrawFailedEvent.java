package com.coinexchange.withdraw.event;

public record WithdrawFailedEvent(
        String reason,
        Long withdrawId
) {
}
