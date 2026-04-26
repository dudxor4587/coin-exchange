package com.coinexchange.trade.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TradeException extends BaseException {

    private final TradeExceptionType tradeExceptionType;

    @Override
    public String errorMessage() {
        return tradeExceptionType.errorMessage();
    }

    @Override
    public BaseExceptionType exceptionType() {
        return tradeExceptionType;
    }
}
