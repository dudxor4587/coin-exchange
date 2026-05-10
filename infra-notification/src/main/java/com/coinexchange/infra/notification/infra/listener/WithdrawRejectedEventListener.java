package com.coinexchange.infra.notification.infra.listener;

import com.coinexchange.events.withdraw.WithdrawRejectedEvent;
import com.coinexchange.infra.notification.application.NotificationSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class WithdrawRejectedEventListener {

    private final NotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "withdraw.rejected", groupId = "notification")
    public void handle(String json) throws JsonProcessingException {
        WithdrawRejectedEvent event = objectMapper.readValue(json, WithdrawRejectedEvent.class);
        log.info("출금 거절 이벤트 수신: userId={}, reason={}", event.userId(), event.reason());
        notificationSender.send(
                event.userId(),
                "거래소에 요청하신 출금이 거절되었습니다. 사유: " + event.reason()
        );
    }
}
