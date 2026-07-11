package com.coinexchange.trading.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.trade.application.TradeService;
import com.coinexchange.trading.application.event.OrderPlacedEvent;
import com.coinexchange.trading.application.event.TradeExecutedEvent;
import com.coinexchange.trading.infra.projection.ProcessedEvent;
import com.coinexchange.trading.infra.projection.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * durable 로그(Kafka)의 이벤트를 DB(사본)에 반영한다.
 *
 * dedup 마커 INSERT와 실제 반영을 한 트랜잭션에 묶는다.
 * at-least-once로 같은 이벤트가 재전달돼도 dedup 체크로 한 번만 반영된다 —
 * 재소비 시 fillOrder가 두 번 더해지는 것 같은 오염을 막는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProjectionService {

    private final OrderService orderService;
    private final TradeService tradeService;
    private final ProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void applyOrderPlaced(OrderPlacedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        if (event.type() == Order.Type.BUY) {
            orderService.createBuyOrder(event.orderId(), event.coinId(), event.price(),
                    event.amount(), event.userId(), event.lockedFunds());
        } else {
            orderService.createSellOrder(event.orderId(), event.coinId(), event.price(),
                    event.amount(), event.userId());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }

    @Transactional
    public void applyTradeExecuted(TradeExecutedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
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

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }
}
