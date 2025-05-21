package com.coinexchange.notification.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSender notificationSender;

    public void sendRejectionNotification(Long userId, String reason) {
        notificationSender.send(userId, reason);
    }
}
