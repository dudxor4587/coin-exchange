package com.coinexchange.user.presentation.dto;

public record SignUpRequest(
        String email,
        String password,
        String name,
        String phoneNumber
) {
}
