package com.coinexchange.withdraw.event;

import java.math.BigDecimal;

public record WithdrawApprovedEvent(
        Long userId,
        BigDecimal amount,
        Long withdrawId
) {
}
