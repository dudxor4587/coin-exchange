package com.coinexchange.trading.presentation.dto;

import java.math.BigDecimal;

public record BuyOrderRequest(
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
