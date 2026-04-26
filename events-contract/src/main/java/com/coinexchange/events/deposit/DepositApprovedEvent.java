package com.coinexchange.events.deposit;

import java.math.BigDecimal;

public record DepositApprovedEvent(
        Long userId,
        BigDecimal amount
) {
}
