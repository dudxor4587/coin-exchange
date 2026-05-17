package com.coinexchange.trading.infra.outbox;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EventToOutboxBridge {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        save("notification.requested", String.valueOf(event.userId()), event);
    }

    private void save(String topic, String partitionKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(OutboxMessage.builder()
                    .topic(topic)
                    .partitionKey(partitionKey)
                    .payload(json)
                    .build());

            // outbox INSERT 사실을 시그널로 발행 — relay가 AFTER_COMMIT에서 받아 wake.
            publisher.publishEvent(new OutboxInsertedSignal());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트 직렬화 실패", e);
        }
    }
}
