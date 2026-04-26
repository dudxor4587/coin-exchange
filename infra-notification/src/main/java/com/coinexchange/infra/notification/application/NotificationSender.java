package com.coinexchange.infra.notification.application;

public interface NotificationSender {

    void send(Long userId, String message);
}
