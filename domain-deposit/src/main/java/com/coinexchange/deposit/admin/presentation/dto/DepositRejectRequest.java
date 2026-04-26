package com.coinexchange.deposit.admin.presentation.dto;

public record DepositRejectRequest(
        Long depositId,
        String reason
) {
}
