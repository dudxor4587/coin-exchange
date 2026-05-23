package com.coinexchange.common.auth.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthException extends BaseException {

    private final AuthExceptionType authExceptionType;

    @Override
    public BaseExceptionType exceptionType() {
        return authExceptionType;
    }

    @Override
    public String errorMessage() {
        return authExceptionType.errorMessage();
    }
}
