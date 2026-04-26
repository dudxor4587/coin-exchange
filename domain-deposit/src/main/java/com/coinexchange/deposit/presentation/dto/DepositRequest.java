package com.coinexchange.deposit.presentation.dto;

import java.math.BigDecimal;

public record DepositRequest(
        BigDecimal amount,
        String bank,
        String accountNumber
) {
}
