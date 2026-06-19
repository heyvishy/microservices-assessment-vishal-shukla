package com.loadup.assessment.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events", schema = "notification_service")
@NoArgsConstructor
public class ProcessedEventEntity {
    @Id
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    public static ProcessedEventEntity of(final UUID eventId) {
        ProcessedEventEntity processedEventEntity = new ProcessedEventEntity();
        processedEventEntity.eventId = eventId;
        processedEventEntity.processedAt = Instant.now();
        return processedEventEntity;
    }
}
