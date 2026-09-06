package com.coinexchange.trading.presentation.dto;

import java.math.BigDecimal;

public record SellOrderRequest(
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
