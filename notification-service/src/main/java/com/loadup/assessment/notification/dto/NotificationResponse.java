package com.loadup.assessment.notification.dto;

import com.loadup.assessment.notification.domain.NotificationEntity;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID orderId,
        String eventType,
        String channel,
        String recipient,
        String message,
        Instant createdAt
) {
    public static NotificationResponse from(final NotificationEntity notificationEntity) {
        return new NotificationResponse(
                notificationEntity.getId(),
                notificationEntity.getOrderId(),
                notificationEntity.getEventType(),
                notificationEntity.getChannel(),
                notificationEntity.getRecipient(),
                notificationEntity.getMessage(),
                notificationEntity.getCreatedAt());
    }
}
