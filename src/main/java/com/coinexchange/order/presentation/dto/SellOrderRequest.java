package com.coinexchange.order.presentation.dto;

import java.math.BigDecimal;

public record SellOrderRequest(
        Long coinId,
        BigDecimal price,
        Long amount
) {
}
