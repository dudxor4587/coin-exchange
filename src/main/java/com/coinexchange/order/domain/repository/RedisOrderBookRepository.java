package com.coinexchange.order.domain.repository;

import com.coinexchange.order.domain.OrderBook;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RedisOrderBookRepository {

    void saveOrder(OrderBook order);

    Optional<OrderBook> findById(Long orderId);

    List<Map<String, Object>> executeScheduledMatch();
}
