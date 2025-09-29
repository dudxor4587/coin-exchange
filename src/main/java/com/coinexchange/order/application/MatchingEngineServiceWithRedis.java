package com.coinexchange.order.application;

import com.coinexchange.order.domain.repository.RedisOrderBookRepository;
import com.coinexchange.trade.event.TradeCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MatchingEngineServiceWithRedis {

    private final RedisOrderBookRepository redisOrderBookRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter completedTradesCounter;
    private final Timer tradeLatencyTimer;

    public MatchingEngineServiceWithRedis(RedisOrderBookRepository redisOrderBookRepository,
                                          ApplicationEventPublisher eventPublisher,
                                          MeterRegistry meterRegistry) {
        this.redisOrderBookRepository = redisOrderBookRepository;
        this.eventPublisher = eventPublisher;

        this.completedTradesCounter = Counter.builder("trade_completed_total")
                .description("Total number of completed trades")
                .register(meterRegistry);

        this.tradeLatencyTimer = Timer.builder("trade_latency_seconds")
                .description("Latency for trade matching")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 500)
    public void matchOrders() {
        List<Map<String, Object>> trades = redisOrderBookRepository.executeScheduledMatch();

        long start = System.nanoTime(); // latency 측정 시작

        if (trades != null && !trades.isEmpty()) {
            for (Map<String, Object> trade : trades) {

                long elapsed = System.nanoTime() - start;
                tradeLatencyTimer.record(elapsed, TimeUnit.NANOSECONDS); // latency 기록
                completedTradesCounter.increment(); // 체결 카운터 증가

                log.info("매칭 체결: 매수={}, 매도={}, 수량={}, 가격={}",
                        Long.valueOf(String.valueOf(trade.get("buyerId"))),
                        Long.valueOf(String.valueOf(trade.get("sellerId"))),
                        Long.valueOf(String.valueOf(trade.get("matchedAmount"))),
                        new BigDecimal(String.valueOf(trade.get("price"))));

                eventPublisher.publishEvent(new TradeCreatedEvent(
                        Long.valueOf(String.valueOf(trade.get("buyOrderId"))),
                        Long.valueOf(String.valueOf(trade.get("sellOrderId"))),
                        Long.valueOf(String.valueOf(trade.get("buyerId"))),
                        Long.valueOf(String.valueOf(trade.get("sellerId"))),
                        Long.valueOf(String.valueOf(trade.get("coinId"))),
                        new BigDecimal(String.valueOf(trade.get("price"))),
                        Long.valueOf(String.valueOf(trade.get("matchedAmount")))
                ));
            }
        }
    }
}
