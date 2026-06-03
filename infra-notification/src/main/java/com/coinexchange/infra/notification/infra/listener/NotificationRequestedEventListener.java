package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.infra.notification.application.NotificationSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestedEventListener {

    private final NotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.requested", groupId = "notification")
    public void handle(String json) throws JsonProcessingException {
        NotificationRequestedEvent event = objectMapper.readValue(json, NotificationRequestedEvent.class);
        log.info("알림 요청 수신: userId={}", event.userId());
        notificationSender.send(event.userId(), event.message());
    }
}
