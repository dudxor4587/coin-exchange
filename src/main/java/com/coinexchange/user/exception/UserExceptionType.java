package com.coinexchange.user.exception;

import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;


@RequiredArgsConstructor
public enum UserExceptionType implements BaseExceptionType {
    USER_NOT_FOUND(NOT_FOUND, "User not found"),
    PASSWORD_MISMATCH(UNAUTHORIZED, "Password mismatch"),
    EMAIL_ALREADY_EXISTS(BAD_REQUEST, "Email already exists"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String errorMessage() {
        return message;
    }
}
