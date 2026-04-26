package com.coinexchange.coin.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CoinException extends BaseException {

    private final CoinExceptionType coinExceptionType;

    @Override
    public BaseExceptionType exceptionType() {
        return coinExceptionType;
    }

    @Override
    public String errorMessage() {
        return coinExceptionType.errorMessage();
    }
}
