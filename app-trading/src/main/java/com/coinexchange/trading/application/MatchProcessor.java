package com.coinexchange.trading.application;

import com.coinexchange.trading.application.event.TradeExecutedEvent;
import com.coinexchange.trading.infra.FundsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 매칭 한 건의 체결 처리.
 * hot path에서는 돈이 걸린 정산(settle, 동기 RPC)까지만 처리하고,
 * Trade INSERT / Order fill / 알림은 TradeExecutedEvent로 넘겨
 * projector가 비동기로 반영한다. 체결의 진실은 이미 매칭 Lua가
 * Redis OrderBook에 반영했으므로(remainingAmount 차감), DB 기록을
 * 여기서 동기로 기다릴 이유가 없다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchProcessor {

    private final FundsClient fundsClient;
    private final ApplicationEventPublisher eventPublisher;

    public void processMatch(Map<String, Object> match) {
        Long buyOrderId = parseLong(match.get("buyOrderId"));
        Long sellOrderId = parseLong(match.get("sellOrderId"));
        Long buyerId = parseLong(match.get("buyerId"));
        Long sellerId = parseLong(match.get("sellerId"));
        Long coinId = parseLong(match.get("coinId"));
        Long matchedAmount = parseLong(match.get("matchedAmount"));
        BigDecimal price = new BigDecimal(String.valueOf(match.get("price")));
        BigDecimal totalKrw = price.multiply(BigDecimal.valueOf(matchedAmount));

        fundsClient.settle(buyerId, sellerId, coinId, matchedAmount, totalKrw);

        eventPublisher.publishEvent(new TradeExecutedEvent(
                buyOrderId, sellOrderId, buyerId, sellerId, coinId, matchedAmount, price));
    }

    private static Long parseLong(Object value) {
        return Long.valueOf(String.valueOf(value));
    }
}
