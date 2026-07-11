package com.coinexchange.matching.domain.repository;

import com.coinexchange.matching.domain.OrderBook;

import java.util.List;
import java.util.Map;

public interface RedisOrderBookRepository {

    void saveOrder(OrderBook order);

    List<Map<String, Object>> executeScheduledMatch();
}
