package com.coinexchange.user.presentation.dto;

public record LoginRequest(
        String email,
        String password
) {
}
