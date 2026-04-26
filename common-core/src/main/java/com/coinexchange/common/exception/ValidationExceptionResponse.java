package com.coinexchange.common.exception;

import java.util.List;

public record ValidationExceptionResponse(
        int status,
        String message,
        List<String> errors
) {
}
