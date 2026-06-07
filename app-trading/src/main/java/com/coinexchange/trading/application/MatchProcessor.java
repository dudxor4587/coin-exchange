package com.coinexchange.trading.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.trade.application.TradeService;
import com.coinexchange.trading.infra.FundsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 매칭 한 건의 체결 처리를 독립 트랜잭션으로 묶는다.
 * OrderFlowService에서 분리한 이유는 두 가지다.
 * 1. placeBuyOrder를 한 트랜잭션으로 묶으면 Order commit 전에 OrderBook(Redis)에
 *    등록되어, 다른 스레드가 OrderBook은 보지만 DB Order는 못 보는 race가 난다.
 *    매칭 처리를 별도 빈으로 빼서 매칭마다 독립 트랜잭션으로 commit 한다.
 * 2. 같은 클래스 내부 호출은 프록시를 거치지 않아 @Transactional이 적용되지 않는다.
 *    별도 빈으로 분리해야 트랜잭션 경계가 실제로 생긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchProcessor {

    private final OrderService orderService;
    private final TradeService tradeService;
    private final FundsClient fundsClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processMatch(Map<String, Object> match) {
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
