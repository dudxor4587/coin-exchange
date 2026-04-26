package com.coinexchange.order.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderException extends BaseException {

    private final OrderExceptionType exceptionType;

    @Override
    public String errorMessage() {
        return exceptionType.errorMessage();
    }

    @Override
    public BaseExceptionType exceptionType() {
        return exceptionType;
    }

}
