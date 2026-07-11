package com.coinexchange.trading.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.trade.application.TradeService;
import com.coinexchange.trading.application.event.OrderPlacedEvent;
import com.coinexchange.trading.application.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Redis(진실)에서 일어난 일을 DB(사본)에 비동기로 반영하는 projector.
 *
 * 주문 접수와 체결의 진실은 이미 Redis OrderBook과 매칭 Lua가 갖고 있다.
 * DB의 Order/Trade는 그 결과를 기록하는 사본이므로, hot path에서 동기로
 * 기다릴 이유가 없다 — 측정에서 createOrder(동기 DB INSERT)가 전체의 76%였다.
 *
 * projectionExecutor는 단일 스레드다. 발행 순서 = 처리 순서가 보장되어,
 * 주문 INSERT가 그 주문의 체결 fill보다 반드시 먼저 실행된다.
 *
 * 알림 발행이 여기 있는 이유: NotificationRequestedEventHandler가
 * AFTER_COMMIT 리스너라 트랜잭션 밖에서 발행하면 조용히 버려진다.
 * projection 트랜잭션 안에서 발행해 "기록이 commit된 후 알림"이 되게 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProjectionHandler {

    private final OrderService orderService;
    private final TradeService tradeService;
    private final ApplicationEventPublisher eventPublisher;

    @Async("projectionExecutor")
    @EventListener
    @Transactional
    public void on(OrderPlacedEvent event) {
        if (event.type() == Order.Type.BUY) {
            orderService.createBuyOrder(event.orderId(), event.coinId(), event.price(),
                    event.amount(), event.userId(), event.lockedFunds());
        } else {
            orderService.createSellOrder(event.orderId(), event.coinId(), event.price(),
                    event.amount(), event.userId());
        }
    }

    @Async("projectionExecutor")
    @EventListener
    @Transactional
    public void on(TradeExecutedEvent event) {
        BigDecimal price = event.price();
        Long matchedAmount = event.matchedAmount();

        tradeService.createTrade(event.buyOrderId(), event.sellOrderId(),
                event.coinId(), matchedAmount, price);

        orderService.fillOrder(event.buyOrderId(), matchedAmount);
        orderService.fillOrder(event.sellOrderId(), matchedAmount);

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                event.buyerId(),
                String.format("매수 체결: 코인 ID=%d, 수량=%d, 가격=%s", event.coinId(), matchedAmount, price)
        ));
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                event.sellerId(),
                String.format("매도 체결: 코인 ID=%d, 수량=%d, 가격=%s", event.coinId(), matchedAmount, price)
        ));
    }
}
