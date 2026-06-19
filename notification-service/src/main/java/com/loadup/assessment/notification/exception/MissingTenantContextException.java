package com.loadup.assessment.notification.exception;

public class MissingTenantContextException extends RuntimeException {
    public MissingTenantContextException(final String message) {
        super(message);
    }
}
