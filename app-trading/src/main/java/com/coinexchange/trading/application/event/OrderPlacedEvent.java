package com.coinexchange.trading.application.event;

import com.coinexchange.order.domain.Order;

import java.math.BigDecimal;

/**
 * 주문이 접수(Redis OrderBook 등록)됐음을 알리는 내부 이벤트.
 * projector가 이걸 받아 Order를 DB에 비동기로 INSERT한다.
 */
public record OrderPlacedEvent(
        Long orderId,
        Long coinId,
        BigDecimal price,
        Long amount,
        Long userId,
        Order.Type type,
        BigDecimal lockedFunds
) {
}
