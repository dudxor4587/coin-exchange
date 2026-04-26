package com.coinexchange.order.exception;

import com.coinexchange.common.exception.BaseException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderBookException extends BaseException {

    private final OrderBookExceptionType orderBookExceptionType;

    @Override
    public String errorMessage() {
        return orderBookExceptionType.errorMessage();
    }

    @Override
    public OrderBookExceptionType exceptionType() {
        return orderBookExceptionType;
    }
}
