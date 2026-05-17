package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.infra.notification.application.NotificationSender;
import com.coinexchange.infra.notification.infra.dedup.ProcessedEvent;
import com.coinexchange.infra.notification.infra.dedup.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestedEventListener {

    private final NotificationSender notificationSender;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.requested", groupId = "notification")
    @Transactional
    public void handle(String json) throws JsonProcessingException {
        NotificationRequestedEvent event = objectMapper.readValue(json, NotificationRequestedEvent.class);

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("이미 처리된 이벤트, skip: eventId={}", event.eventId());
            return;
        }

        log.info("알림 요청 수신: userId={}", event.userId());
        notificationSender.send(event.userId(), event.message());

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }
}
