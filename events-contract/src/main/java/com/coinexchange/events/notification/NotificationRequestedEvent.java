package com.coinexchange.events.notification;

import java.util.UUID;

public record NotificationRequestedEvent(
        UUID eventId,
        Long userId,
        String message
) {
    public NotificationRequestedEvent(Long userId, String message) {
        this(UUID.randomUUID(), userId, message);
    }
}
