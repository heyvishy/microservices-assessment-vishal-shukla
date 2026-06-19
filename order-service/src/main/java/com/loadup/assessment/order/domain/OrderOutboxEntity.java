package com.loadup.assessment.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_outbox", schema = "order_service")
@Getter
@NoArgsConstructor
public class OrderOutboxEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 200)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int publishAttempts;

    @Column(length = 1000)
    private String lastError;

    public static OrderOutboxEntity of(final UUID aggregateId, final String eventType, final String topic, final String eventKey, final String payload) {
        OrderOutboxEntity orderOutboxEntity = new OrderOutboxEntity();
        orderOutboxEntity.id = UUID.randomUUID();
        orderOutboxEntity.aggregateType = "ORDER";
        orderOutboxEntity.aggregateId = aggregateId;
        orderOutboxEntity.eventType = eventType;
        orderOutboxEntity.topic = topic;
        orderOutboxEntity.eventKey = eventKey;
        orderOutboxEntity.payload = payload;
        orderOutboxEntity.createdAt = Instant.now();
        orderOutboxEntity.publishAttempts = 0;
        return orderOutboxEntity;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.publishAttempts++;
        this.lastError = null;
    }

    public void markFailed(final String error) {
        this.publishAttempts++;
        this.lastError = error;
    }
}
