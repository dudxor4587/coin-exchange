package com.coinexchange.funds.infra.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int BATCH_SIZE = 200;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 200)
    @Transactional
    public void poll() {
        List<OutboxMessage> pending = outboxRepository.findPendingForUpdate(PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) return;

        for (OutboxMessage msg : pending) {
            try {
                kafkaTemplate.send(msg.getTopic(), msg.getPartitionKey(), msg.getPayload()).get();
                msg.markPublished();
            } catch (Exception e) {
                log.error("Outbox publish 실패 (id={}): {}", msg.getId(), e.getMessage());
                throw new RuntimeException("Outbox publish 실패", e);
            }
        }
        log.debug("Outbox {}건 발행", pending.size());
    }
}
