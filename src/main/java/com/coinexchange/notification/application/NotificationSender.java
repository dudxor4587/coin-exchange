package com.coinexchange.notification.application;

public interface NotificationSender {

    void send(Long userId, String message);
}
