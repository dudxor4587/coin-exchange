package com.coinexchange.events.withdraw;

import java.math.BigDecimal;

public record WithdrawApprovedEvent(
        Long userId,
        BigDecimal amount,
        Long withdrawId
) {
}
