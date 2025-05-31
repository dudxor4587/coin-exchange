package com.coinexchange.trade.domain.repository;

import com.coinexchange.trade.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
