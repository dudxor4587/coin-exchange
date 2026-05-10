package com.coinexchange.trading.application;

import com.coinexchange.order.application.MatchingEngineServiceWithRedis;
import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.trading.infra.FundsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFlowService {

    private final OrderService orderService;
    private final OrderBookService orderBookService;
    private final MatchingEngineServiceWithRedis matchingEngine;
    private final FundsClient fundsClient;
    private final MatchProcessor matchProcessor;

    public void placeBuyOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        BigDecimal lockedFunds = price.multiply(BigDecimal.valueOf(amount));

        fundsClient.debitKrw(userId, lockedFunds);

        Order order = orderService.createBuyOrder(coinId, price, amount, userId, lockedFunds);
        // Order 트랜잭션 커밋 후 OrderBook에 등록 — Redis와 DB의 race 방지
        orderBookService.placeOrder(order);

        List<Map<String, Object>> matches = matchingEngine.match();
        for (Map<String, Object> match : matches) {
            matchProcessor.processMatch(match);
        }
    }

    public void placeSellOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        fundsClient.debitCoin(userId, coinId, amount);

        Order order = orderService.createSellOrder(coinId, price, amount, userId);
        orderBookService.placeOrder(order);

        List<Map<String, Object>> matches = matchingEngine.match();
        for (Map<String, Object> match : matches) {
            matchProcessor.processMatch(match);
        }
    }
}
