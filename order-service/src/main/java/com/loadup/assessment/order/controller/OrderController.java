package com.loadup.assessment.order.controller;

import com.loadup.assessment.order.dto.CancelOrderRequest;
import com.loadup.assessment.order.dto.OrderCreateRequest;
import com.loadup.assessment.order.dto.OrderResponse;
import com.loadup.assessment.order.dto.OrderUpdateRequest;
import com.loadup.assessment.order.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderApplicationService orderApplicationService;

    public OrderController(final OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody final OrderCreateRequest orderCreateRequest) {
        return OrderResponse.from(orderApplicationService.create(orderCreateRequest));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable final UUID orderId) {
        return OrderResponse.from(orderApplicationService.get(orderId));
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderApplicationService.list().stream().map(OrderResponse::from).toList();
    }

    @PutMapping("/{orderId}")
    public OrderResponse update(@PathVariable final UUID orderId, @Valid @RequestBody final OrderUpdateRequest orderUpdateRequest) {
        return OrderResponse.from(orderApplicationService.update(orderId, orderUpdateRequest));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable final UUID orderId, @Valid @RequestBody(required = false) final CancelOrderRequest cancelOrderRequest) {
        return OrderResponse.from(orderApplicationService.cancel(orderId, cancelOrderRequest == null ? new CancelOrderRequest(null) : cancelOrderRequest));
    }
}
