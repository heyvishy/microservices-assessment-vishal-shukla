package com.loadup.assessment.order.dto;

import com.loadup.assessment.order.domain.OrderEntity;
import com.loadup.assessment.order.constants.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        String customerEmail,
        String description,
        BigDecimal totalAmount,
        String currency,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(final OrderEntity orderEntity) {
        return new OrderResponse(
                orderEntity.getId(),
                orderEntity.getCustomerId(),
                orderEntity.getCustomerEmail(),
                orderEntity.getDescription(),
                orderEntity.getTotalAmount(),
                orderEntity.getCurrency(),
                orderEntity.getStatus(),
                orderEntity.getCreatedAt(),
                orderEntity.getUpdatedAt());
    }
}
