package com.coinexchange.admin.presentation.dto;

public record DepositRejectRequest(
        Long depositId,
        String reason
) {
}
