package com.coinexchange.trade.domain.repository;

import com.coinexchange.trade.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Long countByStatusAndCreatedAtAfter(Trade.Status status, LocalDateTime sinceTime);
}
