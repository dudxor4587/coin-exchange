package com.coinexchange.deposit.event;

import java.math.BigDecimal;

public record DepositApprovedEvent(
        Long userId,
        BigDecimal amount
) {
}
