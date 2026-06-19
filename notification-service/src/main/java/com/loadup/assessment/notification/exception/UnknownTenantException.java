package com.loadup.assessment.notification.exception;

public class UnknownTenantException extends RuntimeException {
    public UnknownTenantException(final String message) {
        super(message);
    }
}
