package com.coinexchange.wallet.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CoinWalletException extends BaseException {

    private final CoinWalletExceptionType coinWalletExceptionType;

    @Override
    public String errorMessage() {
        return coinWalletExceptionType.errorMessage();
    }

    @Override
    public BaseExceptionType exceptionType() {
        return coinWalletExceptionType;
    }
}
