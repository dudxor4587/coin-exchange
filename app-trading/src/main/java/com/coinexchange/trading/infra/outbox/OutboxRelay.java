package com.coinexchange.trading.infra.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Semaphore wakeup = new Semaphore(0);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxInserted(OutboxInsertedSignal signal) {
        wakeup.release();
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduledPoll() {
        drain();
    }

    @Scheduled(fixedDelay = 50)
    public void pumpSignal() {
        if (wakeup.tryAcquire()) {
            drain();
            wakeup.drainPermits();
        }
    }

    @Transactional
    public void drain() {
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
