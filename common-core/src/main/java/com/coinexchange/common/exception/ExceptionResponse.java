package com.coinexchange.common.exception;

public record ExceptionResponse(
        String status,
        String message
) {
}
