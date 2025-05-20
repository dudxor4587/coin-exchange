package com.coinexchange.deposit.exception;

import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@RequiredArgsConstructor
public enum DepositExceptionType implements BaseExceptionType {
    DEPOSIT_NOT_FOUND(NOT_FOUND, "입금 요청 내역을 찾을 수 없습니다."),
    DEPOSIT_STATUS_NOT_PENDING(BAD_REQUEST, "입금 요청 상태가 대기 중이 아닙니다."),
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
