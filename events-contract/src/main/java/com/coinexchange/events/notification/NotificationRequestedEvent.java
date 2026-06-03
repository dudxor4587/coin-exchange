package com.coinexchange.events.notification;

public record NotificationRequestedEvent(
        Long userId,
        String message
) {
}
