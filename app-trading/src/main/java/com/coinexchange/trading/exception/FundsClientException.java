package com.coinexchange.trading.exception;

import com.coinexchange.common.exception.BaseException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FundsClientException extends BaseException {

    private final FundsClientExceptionType fundsClientExceptionType;

    @Override
    public String errorMessage() {
        return fundsClientExceptionType.errorMessage();
    }

    @Override
    public FundsClientExceptionType exceptionType() {
        return fundsClientExceptionType;
    }
}
