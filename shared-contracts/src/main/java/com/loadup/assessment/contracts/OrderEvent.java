package com.loadup.assessment.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String tenantId,
        String customerId,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        Instant occurredAt,
        boolean terminal
) {
}
