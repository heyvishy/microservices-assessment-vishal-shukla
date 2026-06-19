package com.loadup.assessment.order.exception;

public class MissingTenantHeaderException extends RuntimeException {
    public MissingTenantHeaderException(final String message) {
        super(message);
    }
}
