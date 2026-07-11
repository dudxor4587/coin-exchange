package com.coinexchange.matching.application;

import java.math.BigDecimal;

public interface OrderBookService {

    // 매칭엔진은 DB Order 엔티티를 모른다 — 주문의 raw 필드만 받는다.
    void placeOrder(Long orderId, Long coinId, BigDecimal price, Long amount, String side, Long userId);
}
