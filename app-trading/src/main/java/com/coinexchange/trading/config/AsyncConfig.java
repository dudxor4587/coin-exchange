package com.coinexchange.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * DB projection 전용 단일 스레드 executor.
     * 스레드를 하나로 고정하는 이유는 이벤트 처리 순서 보장이다 —
     * 주문 INSERT(OrderPlacedEvent)가 그 주문의 체결 fill(TradeExecutedEvent)보다
     * 반드시 먼저 실행되어야 fillOrder의 findById가 깨지지 않는다.
     * 발행 순서 = 큐 순서 = 처리 순서(FIFO)가 되도록 단일 스레드로 둔다.
     *
     * 의도된 한계: 큐는 인메모리라 프로세스가 죽으면 아직 반영 안 된 projection이
     * 유실되고, 단일 스레드라 지속 부하에서 큐가 쌓일 수 있다. 이 한계는
     * 다음 챕터(durable 로그)에서 다룬다.
     */
    @Bean(name = "projectionExecutor")
    public ThreadPoolTaskExecutor projectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("projection-");
        executor.initialize();
        return executor;
    }
}
