package com.coinexchange.wallet.exception;

import com.coinexchange.common.exception.BaseExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CoinWalletExceptionType implements BaseExceptionType {
    COIN_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "코인 지갑을 찾을 수 없습니다."),
    INSUFFICIENT_COIN_BALANCE(HttpStatus.BAD_REQUEST, "코인 잔액이 부족합니다."),
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
