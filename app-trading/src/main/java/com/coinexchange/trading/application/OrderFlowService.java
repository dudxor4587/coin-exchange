package com.coinexchange.trading.application;

import com.coinexchange.order.application.MatchingEngineServiceWithRedis;
import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.order.infra.RedisOrderIdGenerator;
import com.coinexchange.trading.application.event.OrderPlacedEvent;
import com.coinexchange.trading.infra.FundsClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
public class OrderFlowService {

    // 주문 흐름을 구간별로 쪼개 시간을 측정한다. 매칭(match) 자체가 빠른지,
    // 주변의 동기 RPC(debitKrw/settle)가 병목인지를 데이터로 가르기 위함이다.
    private static final String SEGMENT_METRIC = "order.flow.segment";

    private final OrderBookService orderBookService;
    private final MatchingEngineServiceWithRedis matchingEngine;
    private final FundsClient fundsClient;
    private final MatchProcessor matchProcessor;
    private final RedisOrderIdGenerator orderIdGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public OrderFlowService(OrderBookService orderBookService,
                            MatchingEngineServiceWithRedis matchingEngine,
                            FundsClient fundsClient,
                            MatchProcessor matchProcessor,
                            RedisOrderIdGenerator orderIdGenerator,
                            ApplicationEventPublisher eventPublisher,
                            MeterRegistry meterRegistry) {
        this.orderBookService = orderBookService;
        this.matchingEngine = matchingEngine;
        this.fundsClient = fundsClient;
        this.matchProcessor = matchProcessor;
        this.orderIdGenerator = orderIdGenerator;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    // hot path에는 DB 쓰기가 없다. 주문의 진실은 Redis OrderBook이고,
    // DB Order는 OrderPlacedEvent를 받은 projector가 비동기로 기록하는 사본이다.
    // 측정에서 createOrder(동기 DB INSERT)가 전체의 76%를 차지했던 것을
    // hot path 밖으로 들어낸 구조 변경이다. 돈이 걸린 debit/settle(동기 RPC)은
    // 즉시 정합성이 필요하므로 hot path에 남긴다.
    public void placeBuyOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        BigDecimal lockedFunds = price.multiply(BigDecimal.valueOf(amount));

        timed("debitKrw", () -> fundsClient.debitKrw(userId, lockedFunds));

        Long orderId = timed("nextId", orderIdGenerator::nextId);
        Order order = buildOrder(orderId, coinId, price, amount, userId, Order.Type.BUY, lockedFunds);
        timed("placeOrderBook", () -> orderBookService.placeOrder(order));

        eventPublisher.publishEvent(new OrderPlacedEvent(
                orderId, coinId, price, amount, userId, Order.Type.BUY, lockedFunds));

        List<Map<String, Object>> matches = timed("match", matchingEngine::match);
        timed("processMatches", () -> processMatches(matches));
    }

    public void placeSellOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        timed("debitCoin", () -> fundsClient.debitCoin(userId, coinId, amount));

        Long orderId = timed("nextId", orderIdGenerator::nextId);
        Order order = buildOrder(orderId, coinId, price, amount, userId, Order.Type.SELL, null);
        timed("placeOrderBook", () -> orderBookService.placeOrder(order));

        eventPublisher.publishEvent(new OrderPlacedEvent(
                orderId, coinId, price, amount, userId, Order.Type.SELL, null));

        List<Map<String, Object>> matches = timed("match", matchingEngine::match);
        timed("processMatches", () -> processMatches(matches));
    }

    private Order buildOrder(Long id, Long coinId, BigDecimal price, Long amount,
                             Long userId, Order.Type type, BigDecimal lockedFunds) {
        // OrderBook 등록용 임시 객체 — 영속화는 projector의 몫이다.
        return Order.builder()
                .id(id)
                .coinId(coinId)
                .price(price)
                .orderAmount(amount)
                .filledAmount(0L)
                .lockedFunds(lockedFunds)
                .type(type)
                .userId(userId)
                .status(Order.Status.PENDING)
                .build();
    }

    private void processMatches(List<Map<String, Object>> matches) {
        for (Map<String, Object> match : matches) {
            matchProcessor.processMatch(match);
        }
    }

    private <T> T timed(String segment, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return action.get();
        } finally {
            sample.stop(meterRegistry.timer(SEGMENT_METRIC, "segment", segment));
        }
    }

    private void timed(String segment, Runnable action) {
        timed(segment, () -> {
            action.run();
            return null;
        });
    }
}
