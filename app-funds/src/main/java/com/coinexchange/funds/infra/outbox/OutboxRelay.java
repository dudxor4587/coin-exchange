package com.coinexchange.funds.infra.outbox;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OutboxRelay {

    private static final int BATCH_SIZE = 200;
    private static final long IDLE_POLL_TIMEOUT_SEC = 1L;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate txTemplate;
    private final Semaphore wakeup = new Semaphore(0);

    private volatile boolean running = true;
    private Thread relayThread;

    public OutboxRelay(OutboxRepository outboxRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       PlatformTransactionManager transactionManager) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void start() {
        relayThread = new Thread(this::loop, "outbox-relay");
        relayThread.setDaemon(true);
        relayThread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (relayThread != null) relayThread.interrupt();
    }

    /** outbox INSERT가 commit된 직후 호출 — 릴레이를 즉시 깨운다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxInserted(OutboxInsertedSignal signal) {
        wakeup.release();
    }

    private void loop() {
        while (running) {
            try {
                if (wakeup.tryAcquire(IDLE_POLL_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    wakeup.drainPermits();   // 신호 받았을 때만 백로그 정리
                }
                drainBatch();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Outbox drain 중 오류", e);
            }
        }
    }

    private void drainBatch() {
        txTemplate.executeWithoutResult(status -> {
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
        });
    }
}
