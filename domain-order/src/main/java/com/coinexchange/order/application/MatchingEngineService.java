package com.coinexchange.order.application;

import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.OrderBookRepository;
import com.coinexchange.events.trade.TradeCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(name = "matching.engine", havingValue = "db")
public class MatchingEngineService {

    private final OrderBookRepository orderBookRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final Counter completedTradesCounter;
    private final Timer tradeLatencyTimer;

    public MatchingEngineService(OrderBookRepository orderBookRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 MeterRegistry meterRegistry) {
        this.orderBookRepository = orderBookRepository;
        this.eventPublisher = eventPublisher;

        this.completedTradesCounter = Counter.builder("trade_completed_total")
                .description("Total number of completed trades")
                .register(meterRegistry);

        this.tradeLatencyTimer = Timer.builder("trade_latency_seconds")
                .description("Latency for trade matching")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void matchOrders() {
        List<OrderBook> buyOrders = orderBookRepository
                .findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(
                        OrderBook.Status.ACTIVE, OrderBook.Type.BUY, 0L);
        List<OrderBook> sellOrders = orderBookRepository
                .findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(
                        OrderBook.Status.ACTIVE, OrderBook.Type.SELL, 0L);

        for (OrderBook buy : buyOrders) {
            for (OrderBook sell : sellOrders) {
                if (buy.getPrice().compareTo(sell.getPrice()) == 0 && !buy.getUserId().equals(sell.getUserId())) {
                    Long matchedAmount = Math.min(buy.getRemainingAmount(), sell.getRemainingAmount());

                    long start = System.nanoTime();

                    buy.decreaseAmount(matchedAmount);
                    sell.decreaseAmount(matchedAmount);

                    if (buy.isEmpty()) {
                        buy.complete();
                    }
                    if (sell.isEmpty()) {
                        sell.complete();
                    }

                    orderBookRepository.save(buy);
                    orderBookRepository.save(sell);

                    long elapsed = System.nanoTime() - start;
                    tradeLatencyTimer.record(elapsed, TimeUnit.NANOSECONDS);
                    completedTradesCounter.increment();

                    log.info("매칭 체결: 매수호가 ID={}, 매도호가 ID={}, 체결수량={}, 가격={}",
                            buy.getId(), sell.getId(), matchedAmount, buy.getPrice());

                    eventPublisher.publishEvent(new TradeCreatedEvent(
                            buy.getId(),
                            sell.getId(),
                            buy.getUserId(),
                            sell.getUserId(),
                            buy.getCoinId(),
                            buy.getPrice(),
                            matchedAmount
                    ));
                }
            }
        }
    }
}
