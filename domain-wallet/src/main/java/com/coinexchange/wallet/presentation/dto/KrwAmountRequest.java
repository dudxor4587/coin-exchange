package com.coinexchange.wallet.presentation.dto;

import java.math.BigDecimal;

public record KrwAmountRequest(
        Long userId,
        BigDecimal amount
) {
}
