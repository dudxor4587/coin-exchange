//package com.coinexchange.order.application;
//
//import com.coinexchange.order.domain.OrderBook;
//import com.coinexchange.order.domain.repository.OrderBookRepository;
//import com.coinexchange.trade.event.TradeCreatedEvent;
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//@Service
//@Slf4j
//public class MatchingEngineService {
//
//    private final OrderBookRepository orderBookRepository;
//    private final ApplicationEventPublisher eventPublisher;
//
//    private final Counter completedTradesCounter;
//    private final Timer tradeLatencyTimer;
//
//    public MatchingEngineService(OrderBookRepository orderBookRepository,
//                                 ApplicationEventPublisher eventPublisher,
//                                 MeterRegistry meterRegistry) {
//        this.orderBookRepository = orderBookRepository;
//        this.eventPublisher = eventPublisher;
//
//        // Metric 초기화
//        this.completedTradesCounter = Counter.builder("trade_completed_total")
//                .description("Total number of completed trades")
//                .register(meterRegistry);
//
//        this.tradeLatencyTimer = Timer.builder("trade_latency_seconds")
//                .description("Latency for trade matching")
//                .register(meterRegistry);
//    }
//
////    @Scheduled(fixedDelay = 500) // 0.5초마다 매칭 시도
//    @Transactional
//    public void matchOrders() {
//        List<OrderBook> buyOrders = orderBookRepository
//                .findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(
//                        OrderBook.Status.ACTIVE, OrderBook.Type.BUY, 0L);
//        List<OrderBook> sellOrders = orderBookRepository
//                .findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(
//                        OrderBook.Status.ACTIVE, OrderBook.Type.SELL, 0L);
//
//        for (OrderBook buy : buyOrders) {
//            for (OrderBook sell : sellOrders) {
//                if (buy.getPrice().compareTo(sell.getPrice()) == 0 && !buy.getUserId().equals(sell.getUserId())) {
//                    Long matchedAmount = Math.min(buy.getRemainingAmount(), sell.getRemainingAmount());
//
//                    long start = System.nanoTime(); // latency 측정 시작
//
//                    buy.decreaseAmount(matchedAmount);
//                    sell.decreaseAmount(matchedAmount);
//
//                    orderBookRepository.save(buy);
//                    orderBookRepository.save(sell);
//
//                    long elapsed = System.nanoTime() - start;
//                    tradeLatencyTimer.record(elapsed, TimeUnit.NANOSECONDS); // latency 기록
//                    completedTradesCounter.increment(); // 체결 카운터 증가
//
//                    log.info("매칭 체결: 매수호가 ID={}, 매도호가 ID={}, 체결수량={}, 가격={}",
//                            buy.getId(), sell.getId(), matchedAmount, buy.getPrice());
//
//                    eventPublisher.publishEvent(new TradeCreatedEvent(
//                            buy.getId(),
//                            sell.getId(),
//                            buy.getUserId(),
//                            sell.getUserId(),
//                            buy.getCoinId(),
//                            buy.getPrice(),
//                            matchedAmount
//                    ));
//
//                    if (buy.isEmpty()) {
//                        buy.complete();
//                    }
//                    if (sell.isEmpty()) {
//                        sell.complete();
//                    }
//                }
//            }
//        }
//    }
//}
