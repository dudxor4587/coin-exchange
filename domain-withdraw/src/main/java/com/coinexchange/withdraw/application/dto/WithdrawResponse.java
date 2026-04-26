package com.coinexchange.withdraw.application.dto;

import java.math.BigDecimal;

public record WithdrawResponse(
        String status,
        BigDecimal amount,
        String message
) {
}
