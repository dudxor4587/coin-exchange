package com.coinexchange.user.exception;

import com.coinexchange.common.exception.BaseException;
import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WalletException extends BaseException {

    private final WalletExceptionType walletExceptionType;

    @Override
    public BaseExceptionType exceptionType() {
        return walletExceptionType;
    }

}
