package com.loadup.assessment.order.exception;

public class OrderConflictException extends RuntimeException {
    public OrderConflictException(final String message) {
        super(message);
    }
}
