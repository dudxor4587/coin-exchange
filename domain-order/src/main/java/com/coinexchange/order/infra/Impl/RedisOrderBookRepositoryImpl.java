package com.coinexchange.order.infra.Impl;

import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.RedisOrderBookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Repository
@Slf4j
public class RedisOrderBookRepositoryImpl implements RedisOrderBookRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> scheduledMatchScript;

    public RedisOrderBookRepositoryImpl(RedisTemplate<String, Object> redisTemplate) throws IOException {
        this.redisTemplate = redisTemplate;

        ResourceScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("scripts/matchingLogic.lua"));
        this.scheduledMatchScript = new DefaultRedisScript<>(scriptSource.getScriptAsString(), List.class);
    }

    @Override
    public void saveOrder(OrderBook order) {
        String key = "orderbook:" + order.getId();
        Map<String, String> map = new HashMap<>();
        map.put("id", order.getId().toString());
        map.put("userId", order.getUserId().toString());
        map.put("coinId", order.getCoinId().toString());
        map.put("price", order.getPrice().toString());
        map.put("orderId", order.getId().toString());
        map.put("remainingAmount", order.getRemainingAmount().toString());
        map.put("type", order.getType().name());

        redisTemplate.opsForHash().putAll(key, map);

        String priceSetKey = "orderbook:" + order.getType().name() + ":" + order.getPrice();
        redisTemplate.opsForSet().add(priceSetKey, order.getId().toString());
        redisTemplate.opsForSet().add("prices:" + order.getType().name(), order.getPrice().toString());
    }

    @Override
    public Optional<OrderBook> findById(Long orderId) {
        String key = "orderbook:" + orderId;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        if (map.isEmpty()) return Optional.empty();

        OrderBook orderbook = OrderBook.builder()
                .id(Long.valueOf((String) map.get("id")))
                .userId(Long.valueOf((String) map.get("userId")))
                .coinId(Long.valueOf((String) map.get("coinId")))
                .price(new BigDecimal((String) map.get("price")))
                .remainingAmount(Long.valueOf((String) map.get("remainingAmount")))
                .orderId(Long.valueOf((String) map.get("orderId")))
                .type(OrderBook.Type.valueOf((String) map.get("type")))
                .build();

        return Optional.of(orderbook);
    }

    @Override
    public List<Map<String, Object>> executeScheduledMatch() {
        List<String> keys = List.of("prices:BUY", "prices:SELL");

        List<List<Object>> rawResult = (List<List<Object>>) redisTemplate.execute(
                scheduledMatchScript,
                keys
        );

        if (rawResult.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("rawResult from script: {}", rawResult);

        // Lua 스크립트의 결과를 List<Map<String, Object>> 형태로 변환
        List<Map<String, Object>> trades = new ArrayList<>();
        for (List<Object> tradeData : rawResult) {
            Map<String, Object> tradeMap = new HashMap<>();
            for (int i = 0; i < tradeData.size(); i += 2) {
                // Key는 String, Value는 Object로 변환하여 Map에 저장
                tradeMap.put((String) tradeData.get(i), tradeData.get(i + 1));
            }
            trades.add(tradeMap);
        }
        log.info("trades after script execution: {}", trades);

        // 스크립트 실행 및 결과 반환
        return trades;
    }
}
