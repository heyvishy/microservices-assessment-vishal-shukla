package com.loadup.assessment.order.dto;

import com.loadup.assessment.order.constants.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderUpdateRequest(
        @NotNull(message = "status is required") OrderStatus status,
        String description
) {
}
