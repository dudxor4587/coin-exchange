package com.coinexchange.withdraw.presentation.dto;

import java.math.BigDecimal;

public record WithdrawRequest(
        BigDecimal amount,
        String bank,
        String accountNumber
) {
}
