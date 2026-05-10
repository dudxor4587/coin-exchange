package com.coinexchange.trade.application;

import com.coinexchange.trade.domain.Trade;
import com.coinexchange.trade.domain.repository.TradeRepository;
import com.coinexchange.trade.exception.TradeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.trade.exception.TradeExceptionType.TRADE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public Trade createTrade(Long buyOrderId, Long sellOrderId, Long coinId, Long amount, BigDecimal price) {
        Trade trade = Trade.builder()
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .coinId(coinId)
                .amount(amount)
                .price(price)
                .build();
        Trade saved = tradeRepository.save(trade);
        log.info("거래 생성: tradeId={}, buyOrder={}, sellOrder={}, amount={}",
                saved.getId(), buyOrderId, sellOrderId, amount);
        return saved;
    }

    @Transactional
    public void handleOrderMatchFailure(Long tradeId, String reason) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new TradeException(TRADE_NOT_FOUND));
        trade.updateFailedInfo(reason);
        tradeRepository.save(trade);
        log.info("거래 정보 복구 완료: tradeId={}", tradeId);
    }
}
