package com.coinexchange.trading.application;

import com.coinexchange.order.application.MatchingEngineServiceWithRedis;
import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.trading.infra.FundsClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
public class OrderFlowService {

    // 주문 흐름을 구간별로 쪼개 시간을 측정한다. 매칭(match) 자체가 빠른지,
    // 주변의 동기 RPC(debitKrw/settle)와 DB가 병목인지를 데이터로 가르기 위함이다.
    private static final String SEGMENT_METRIC = "order.flow.segment";

    private final OrderService orderService;
    private final OrderBookService orderBookService;
    private final MatchingEngineServiceWithRedis matchingEngine;
    private final FundsClient fundsClient;
    private final MatchProcessor matchProcessor;
    private final MeterRegistry meterRegistry;

    public OrderFlowService(OrderService orderService,
                            OrderBookService orderBookService,
                            MatchingEngineServiceWithRedis matchingEngine,
                            FundsClient fundsClient,
                            MatchProcessor matchProcessor,
                            MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.orderBookService = orderBookService;
        this.matchingEngine = matchingEngine;
        this.fundsClient = fundsClient;
        this.matchProcessor = matchProcessor;
        this.meterRegistry = meterRegistry;
    }

    // 전체에 @Transactional을 걸지 않는다. createBuyOrder가 자체 트랜잭션으로 Order를
    // commit한 뒤에 OrderBook(Redis)에 등록해야, 다른 스레드가 OrderBook은 보지만
    // DB Order는 못 보는 race를 피할 수 있다.
    public void placeBuyOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        BigDecimal lockedFunds = price.multiply(BigDecimal.valueOf(amount));

        timed("debitKrw", () -> fundsClient.debitKrw(userId, lockedFunds));

        Order order = timed("createOrder",
                () -> orderService.createBuyOrder(coinId, price, amount, userId, lockedFunds));
        timed("placeOrderBook", () -> orderBookService.placeOrder(order));

        List<Map<String, Object>> matches = timed("match", matchingEngine::match);
        timed("processMatches", () -> processMatches(matches));
    }

    public void placeSellOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        timed("debitCoin", () -> fundsClient.debitCoin(userId, coinId, amount));

        Order order = timed("createOrder",
                () -> orderService.createSellOrder(coinId, price, amount, userId));
        timed("placeOrderBook", () -> orderBookService.placeOrder(order));

        List<Map<String, Object>> matches = timed("match", matchingEngine::match);
        timed("processMatches", () -> processMatches(matches));
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
