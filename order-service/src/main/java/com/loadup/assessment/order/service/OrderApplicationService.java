package com.loadup.assessment.order.service;

import com.loadup.assessment.order.dto.CancelOrderRequest;
import com.loadup.assessment.order.dto.OrderCreateRequest;
import com.loadup.assessment.order.dto.OrderUpdateRequest;
import com.loadup.assessment.order.domain.OrderEntity;

import java.util.List;
import java.util.UUID;

public interface OrderApplicationService {
    OrderEntity create(final OrderCreateRequest orderCreateRequest);

    OrderEntity get(final UUID orderId);

    List<OrderEntity> list();

    OrderEntity update(final UUID orderId, final OrderUpdateRequest orderUpdateRequest);

    OrderEntity cancel(final UUID orderId, final CancelOrderRequest cancelOrderRequest);
}
