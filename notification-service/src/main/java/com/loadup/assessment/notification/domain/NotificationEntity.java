package com.loadup.assessment.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification_service")
@Getter
@NoArgsConstructor
public class NotificationEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static NotificationEntity create(final UUID orderId, final String eventType, final String channel, final String recipient, final String message) {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.id = UUID.randomUUID();
        notificationEntity.orderId = orderId;
        notificationEntity.eventType = eventType;
        notificationEntity.channel = channel;
        notificationEntity.recipient = recipient;
        notificationEntity.message = message;
        notificationEntity.createdAt = Instant.now();
        return notificationEntity;
    }
}
