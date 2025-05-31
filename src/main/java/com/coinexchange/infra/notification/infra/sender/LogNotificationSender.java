package com.coinexchange.infra.notification.infra.sender;

import com.coinexchange.infra.notification.application.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 로그 자체는 infra에 관련이 없지만,
// 이해를 돕기 위해 infra에 위치시켰습니다. 실제로는 infra에 위치할 필요는 없습니다.
@Component
@Slf4j
public class LogNotificationSender implements NotificationSender {

    @Override
    public void send(Long userId, String message) {
        log.info("알림 전송: userId={}, {}", userId, message);
    }
}
