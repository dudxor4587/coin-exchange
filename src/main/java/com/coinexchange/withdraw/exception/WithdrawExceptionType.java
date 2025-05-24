package com.coinexchange.withdraw.exception;

import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
public enum WithdrawExceptionType implements BaseExceptionType {

    WITHDRAW_NOT_FOUND(NOT_FOUND, "출금 요청 내역을 찾을 수 없습니다."),
    WITHDRAW_STATUS_NOT_PENDING(BAD_REQUEST, "출금 요청 상태가 '대기중'이 아닙니다."),
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
