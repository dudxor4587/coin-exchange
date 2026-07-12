package com.coinexchange.trading.exception;

import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RequiredArgsConstructor
public enum FundsClientExceptionType implements BaseExceptionType {

    FUNDS_UNAVAILABLE(SERVICE_UNAVAILABLE, "결제 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요."),
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
