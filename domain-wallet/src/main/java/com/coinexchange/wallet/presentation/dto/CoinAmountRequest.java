package com.coinexchange.wallet.presentation.dto;

public record CoinAmountRequest(
        Long userId,
        Long coinId,
        Long amount
) {
}
