package com.coinexchange.deposit.application.dto;

import java.math.BigDecimal;

public record DepositResponse(
        String status,
        BigDecimal amount,
        String message
) {
}
