package com.coinexchange.common.exception;

public abstract class BaseException extends RuntimeException {

    public BaseException() {
    }

    public abstract String errorMessage();

    public abstract BaseExceptionType exceptionType();

    @Override
    public String getMessage() {
        return errorMessage();
    }
}
