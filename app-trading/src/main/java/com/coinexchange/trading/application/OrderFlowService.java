package com.coinexchange.trading.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.order.application.MatchingEngineServiceWithRedis;
import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.trade.application.TradeService;
import com.coinexchange.trading.infra.FundsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFlowService {

    private final OrderService orderService;
    private final TradeService tradeService;
    private final OrderBookService orderBookService;
    private final MatchingEngineServiceWithRedis matchingEngine;
    private final FundsClient fundsClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void placeBuyOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        BigDecimal lockedFunds = price.multiply(BigDecimal.valueOf(amount));

        fundsClient.debitKrw(userId, lockedFunds);

        Order order = orderService.createBuyOrder(coinId, price, amount, userId, lockedFunds);
        orderBookService.placeOrder(order);

        processMatches(matchingEngine.match());
    }

    @Transactional
    public void placeSellOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        fundsClient.debitCoin(userId, coinId, amount);

        Order order = orderService.createSellOrder(coinId, price, amount, userId);
        orderBookService.placeOrder(order);

        processMatches(matchingEngine.match());
    }

    private void processMatches(List<Map<String, Object>> matches) {
        for (Map<String, Object> match : matches) {
            processMatch(match);
        }
    }

    private void processMatch(Map<String, Object> match) {
        Long buyOrderId = parseLong(match.get("buyOrderId"));
        Long sellOrderId = parseLong(match.get("sellOrderId"));
        Long buyerId = parseLong(match.get("buyerId"));
        Long sellerId = parseLong(match.get("sellerId"));
        Long coinId = parseLong(match.get("coinId"));
        Long matchedAmount = parseLong(match.get("matchedAmount"));
        BigDecimal price = new BigDecimal(String.valueOf(match.get("price")));
        BigDecimal totalKrw = price.multiply(BigDecimal.valueOf(matchedAmount));

        tradeService.createTrade(buyOrderId, sellOrderId, coinId, matchedAmount, price);

        fundsClient.settle(buyerId, sellerId, coinId, matchedAmount, totalKrw);

        orderService.fillOrder(buyOrderId, matchedAmount);
        orderService.fillOrder(sellOrderId, matchedAmount);

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                buyerId,
                String.format("매수 체결: 코인 ID=%d, 수량=%d, 가격=%s", coinId, matchedAmount, price)
        ));
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                sellerId,
                String.format("매도 체결: 코인 ID=%d, 수량=%d, 가격=%s", coinId, matchedAmount, price)
        ));
    }

    private static Long parseLong(Object value) {
        return Long.valueOf(String.valueOf(value));
    }
}
