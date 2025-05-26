package com.coinexchange.coin.domain.repository;

import com.coinexchange.coin.domain.Coin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoinRepository extends JpaRepository<Coin, Long> {
    Optional<Coin> findBySymbol(Coin.Symbol symbol);
}
