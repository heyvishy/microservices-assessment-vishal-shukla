package com.loadup.assessment.order.exception;

public class UnknownTenantException extends RuntimeException {
    public UnknownTenantException(final String message) {
        super(message);
    }
}
