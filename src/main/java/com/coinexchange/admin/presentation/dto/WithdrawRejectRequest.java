package com.coinexchange.admin.presentation.dto;

public record WithdrawRejectRequest(
        Long withdrawId,
        String reason
) {
}
