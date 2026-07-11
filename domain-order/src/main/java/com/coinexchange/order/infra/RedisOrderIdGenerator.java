package com.coinexchange.order.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 주문 id 채번기. DB auto-increment 대신 Redis INCR로 id를 만든다.
 * 주문 저장(DB)이 hot path에서 비동기로 빠지면서, OrderBook(Redis) 등록 시점에
 * DB를 거치지 않고 id가 필요해졌기 때문이다. INCR는 원자적이라 동시 요청에도
 * 중복 없이 단조 증가하는 id를 준다.
 */
@Component
@RequiredArgsConstructor
public class RedisOrderIdGenerator {

    private static final String ORDER_SEQ_KEY = "order:seq";

    private final RedisTemplate<String, Object> redisTemplate;

    public Long nextId() {
        return redisTemplate.opsForValue().increment(ORDER_SEQ_KEY);
    }
}
