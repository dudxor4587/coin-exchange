package com.coinexchange.order.application;

import com.coinexchange.order.domain.repository.RedisOrderBookRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(name = "matching.engine", havingValue = "redis", matchIfMissing = true)
public class MatchingEngineServiceWithRedis {

    private final RedisOrderBookRepository redisOrderBookRepository;
    private final Counter completedTradesCounter;
    private final Timer tradeLatencyTimer;

    public MatchingEngineServiceWithRedis(RedisOrderBookRepository redisOrderBookRepository,
                                          MeterRegistry meterRegistry) {
        this.redisOrderBookRepository = redisOrderBookRepository;

        this.completedTradesCounter = Counter.builder("trade_completed_total")
                .description("Total number of completed trades")
                .register(meterRegistry);

        this.tradeLatencyTimer = Timer.builder("trade_latency_seconds")
                .description("Latency for trade matching")
                .register(meterRegistry);
    }

    public List<Map<String, Object>> match() {
        long start = System.nanoTime();
        List<Map<String, Object>> trades = redisOrderBookRepository.executeScheduledMatch();

        if (trades != null && !trades.isEmpty()) {
            long elapsed = System.nanoTime() - start;
            tradeLatencyTimer.record(elapsed, TimeUnit.NANOSECONDS);
            for (int i = 0; i < trades.size(); i++) {
                completedTradesCounter.increment();
            }
            log.info("매칭 {}건 체결", trades.size());
        }
        return trades != null ? trades : List.of();
    }
}
