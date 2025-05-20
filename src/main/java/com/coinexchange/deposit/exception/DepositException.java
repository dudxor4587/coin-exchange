package com.coinexchange.deposit.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DepositException extends BaseException {

    private final DepositExceptionType depositExceptionType;

    @Override
    public BaseExceptionType exceptionType() {
        return depositExceptionType;
    }
}
