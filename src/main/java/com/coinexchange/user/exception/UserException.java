package com.coinexchange.user.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserException extends BaseException {

    private final UserExceptionType userExceptionType;

    @Override
    public BaseExceptionType exceptionType() {
        return userExceptionType;
    }

    @Override
    public String errorMessage() {
        return userExceptionType.errorMessage();
    }
}
