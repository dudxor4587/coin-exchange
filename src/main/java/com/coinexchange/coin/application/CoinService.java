package com.coinexchange.coin.application;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.coin.domain.repository.CoinRepository;
import com.coinexchange.coin.exception.CoinException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.coinexchange.coin.exception.CoinExceptionType.COIN_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinRepository coinRepository;

    public Coin findBySymbol(String symbol) {
        return coinRepository.findBySymbol(Coin.Symbol.valueOf(symbol))
                .orElseThrow(() -> new CoinException(COIN_NOT_FOUND));
    }
}
