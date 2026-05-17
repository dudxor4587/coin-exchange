package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.deposit.DepositRejectedEvent;
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
@Slf4j
@RequiredArgsConstructor
public class DepositRejectedEventListener {

    private final NotificationSender notificationSender;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "deposit.rejected", groupId = "notification")
    @Transactional
    public void handle(String json) throws JsonProcessingException {
        DepositRejectedEvent event = objectMapper.readValue(json, DepositRejectedEvent.class);

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("이미 처리된 이벤트, skip: eventId={}", event.eventId());
            return;
        }

        log.info("입금 거절 이벤트 수신: userId={}, reason={}", event.userId(), event.reason());
        notificationSender.send(
                event.userId(),
                "거래소에 요청하신 입금이 거절되었습니다. 사유: " + event.reason()
        );

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }
}
