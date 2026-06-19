package com.loadup.assessment.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadup.assessment.contracts.OrderEvent;
import com.loadup.assessment.contracts.OrderEventType;
import com.loadup.assessment.order.dto.CancelOrderRequest;
import com.loadup.assessment.order.dto.OrderCreateRequest;
import com.loadup.assessment.order.dto.OrderUpdateRequest;
import com.loadup.assessment.order.domain.OrderEntity;
import com.loadup.assessment.order.domain.OrderOutboxEntity;
import com.loadup.assessment.order.constants.OrderStatus;
import com.loadup.assessment.order.exception.OrderConflictException;
import com.loadup.assessment.order.repository.OrderOutboxRepository;
import com.loadup.assessment.order.repository.OrderRepository;
import com.loadup.assessment.order.service.OrderApplicationService;
import com.loadup.assessment.order.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationServiceImpl implements OrderApplicationService {
    private static final String TOPIC = "orders.events.v1";

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderApplicationServiceImpl(final OrderRepository orderRepository,
                                       final OrderOutboxRepository orderOutboxRepository,
                                       final ObjectMapper objectMapper,
                                       @Value("${app.kafka.order-topic:" + TOPIC + "}") final String topic) {
        this.orderRepository = orderRepository;
        this.orderOutboxRepository = orderOutboxRepository;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    @Transactional
    public OrderEntity create(final OrderCreateRequest orderCreateRequest) {
        TenantContext.requireTenantId();// todo : This should ideally be handled at gateway layer
        OrderEntity orderEntity = OrderEntity.create(orderCreateRequest.customerId(), orderCreateRequest.customerEmail(),
                orderCreateRequest.description(), orderCreateRequest.totalAmount(), orderCreateRequest.currency());
        OrderEntity savedOrderEntity = orderRepository.saveAndFlush(orderEntity);
        saveOutbox(savedOrderEntity, OrderEventType.ORDER_CREATED);
        return savedOrderEntity;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderEntity get(final UUID orderId) {
        TenantContext.requireTenantId();
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> list() {
        TenantContext.requireTenantId();
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public OrderEntity update(final UUID orderId, final OrderUpdateRequest orderUpdateRequest) {
        TenantContext.requireTenantId();
        OrderEntity orderEntity = get(orderId);
        validateTransition(orderEntity, orderUpdateRequest.status());
        orderEntity.update(orderUpdateRequest.status(), orderUpdateRequest.description());
        orderRepository.save(orderEntity);
        saveOutbox(orderEntity, OrderEventType.ORDER_UPDATED);
        return orderEntity;
    }

    @Override
    @Transactional
    public OrderEntity cancel(final UUID orderId, final CancelOrderRequest request) {
        TenantContext.requireTenantId();
        OrderEntity orderEntity = get(orderId);
        if (!orderEntity.canCancel()) {
            throw new OrderConflictException("Order cannot be canceled from status " + orderEntity.getStatus());
        }
        orderEntity.cancel();
        orderRepository.save(orderEntity);
        saveOutbox(orderEntity, OrderEventType.ORDER_CANCELED);
        return orderEntity;
    }

    private void validateTransition(final OrderEntity orderEntity, final OrderStatus newStatus) {
        if (orderEntity.isTerminal()) {
            throw new OrderConflictException("Terminal orders cannot be updated");
        }
        if (orderEntity.getStatus() == OrderStatus.CANCELED && newStatus != OrderStatus.CANCELED) {
            throw new OrderConflictException("Canceled orders cannot transition to another status");
        }
    }

    private void saveOutbox(final OrderEntity orderEntity, final OrderEventType eventType) {
        String tenantId = TenantContext.requireTenantId();
        OrderEvent event = new OrderEvent(
                UUID.randomUUID(),
                eventType.name(),
                orderEntity.getId(),
                tenantId,
                orderEntity.getCustomerId(),
                orderEntity.getCustomerEmail(),
                com.loadup.assessment.contracts.OrderStatus.valueOf(orderEntity.getStatus().name()),
                orderEntity.getTotalAmount(),
                orderEntity.getCurrency(),
                java.time.Instant.now(),
                orderEntity.isTerminal());
        try {
            orderOutboxRepository.save(OrderOutboxEntity.of(orderEntity.getId(), eventType.name(), topic,
                    orderEntity.getId().toString(), objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize order event", ex);
        }
    }
}
