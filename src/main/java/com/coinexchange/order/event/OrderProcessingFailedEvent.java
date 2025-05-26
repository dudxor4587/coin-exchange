package com.coinexchange.order.event;

public record OrderProcessingFailedEvent(
        Long orderId,
        String reason
) {
}
