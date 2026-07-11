package com.coinexchange.trading.application.event;

import java.math.BigDecimal;

/**
 * 매칭이 체결됐음을 알리는 내부 이벤트.
 * hot path에서는 정산(settle)까지만 동기로 끝내고,
 * Trade INSERT / Order fill / 알림 발행은 projector가 비동기로 처리한다.
 */
public record TradeExecutedEvent(
        Long buyOrderId,
        Long sellOrderId,
        Long buyerId,
        Long sellerId,
        Long coinId,
        Long matchedAmount,
        BigDecimal price
) {
}
