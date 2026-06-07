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

    // 전체에 @Transactional을 걸지 않는다. createBuyOrder가 자체 트랜잭션으로 Order를
    // commit한 뒤에 OrderBook(Redis)에 등록해야, 다른 스레드가 OrderBook은 보지만
    // DB Order는 못 보는 race를 피할 수 있다.
    public void placeBuyOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        BigDecimal lockedFunds = price.multiply(BigDecimal.valueOf(amount));

        fundsClient.debitKrw(userId, lockedFunds);

        Order order = orderService.createBuyOrder(coinId, price, amount, userId, lockedFunds);
        orderBookService.placeOrder(order);

        processMatches(matchingEngine.match());
    }

    public void placeSellOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        fundsClient.debitCoin(userId, coinId, amount);

        Order order = orderService.createSellOrder(coinId, price, amount, userId);
        orderBookService.placeOrder(order);

        processMatches(matchingEngine.match());
    }

    private void processMatches(List<Map<String, Object>> matches) {
        for (Map<String, Object> match : matches) {
            matchProcessor.processMatch(match);
        }
    }
}
