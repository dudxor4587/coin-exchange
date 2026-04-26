package com.coinexchange.events.order;

public record OrderProcessingFailedEvent(
        Long orderId,
        String reason
) {
}
