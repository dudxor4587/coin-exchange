package com.coinexchange.withdraw.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawException extends BaseException {

    private final WithdrawExceptionType withdrawExceptionType;

    @Override
    public BaseExceptionType exceptionType() {
        return withdrawExceptionType;
    }

    @Override
    public String errorMessage() {
        return withdrawExceptionType.errorMessage();
    }

}
