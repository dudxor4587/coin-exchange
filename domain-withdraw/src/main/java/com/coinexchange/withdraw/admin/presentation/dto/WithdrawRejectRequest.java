package com.coinexchange.withdraw.admin.presentation.dto;

public record WithdrawRejectRequest(
        Long withdrawId,
        String reason
) {
}
