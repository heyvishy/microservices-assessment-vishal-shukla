package com.loadup.assessment.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.loadup.assessment.order.constants.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_service")
@Getter
@NoArgsConstructor
public class OrderEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String customerId;

    @Column(nullable = false, length = 255)
    private String customerEmail;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static OrderEntity create(final String customerId, final String customerEmail, final String description,
                                     final BigDecimal totalAmount, final String currency) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.id = UUID.randomUUID();
        orderEntity.customerId = customerId;
        orderEntity.customerEmail = customerEmail;
        orderEntity.description = description;
        orderEntity.totalAmount = totalAmount;
        orderEntity.currency = currency.toUpperCase();
        orderEntity.status = OrderStatus.CONFIRMED;
        return orderEntity;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(final OrderStatus status, final String description) {
        this.status = status;
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }

    public boolean canCancel() {
        return status == OrderStatus.DRAFT || status == OrderStatus.CONFIRMED || status == OrderStatus.PROCESSING;
    }

    public boolean isTerminal() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }
}
