package com.loadup.assessment.order.exception;

public class MissingTenantContextException extends RuntimeException {
    public MissingTenantContextException(final String message) {
        super(message);
    }
}
