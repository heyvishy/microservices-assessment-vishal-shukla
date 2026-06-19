package com.loadup.assessment.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderCreateRequest(
        @NotBlank(message = "customerId is required")
        String customerId,
        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail format is incorrect")
        String customerEmail,
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotBlank String currency
) {
}
