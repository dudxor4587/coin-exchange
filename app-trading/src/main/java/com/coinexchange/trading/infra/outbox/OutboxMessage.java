package com.coinexchange.trading.infra.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox_message", indexes = {
        @Index(name = "idx_outbox_status_id", columnList = "status, id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(name = "partition_key", length = 128)
    private String partitionKey;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    public enum Status { PENDING, PUBLISHED }

    @Builder
    public OutboxMessage(String topic, String partitionKey, String payload) {
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.status = Status.PUBLISHED;
        this.publishedAt = Instant.now();
    }
}
