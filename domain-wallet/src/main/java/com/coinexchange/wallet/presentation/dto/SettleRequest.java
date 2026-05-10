package com.coinexchange.wallet.presentation.dto;

import java.math.BigDecimal;

public record SettleRequest(
        Long buyerId,
        Long sellerId,
        Long coinId,
        Long matchedAmount,
        BigDecimal totalKrw
) {
}
