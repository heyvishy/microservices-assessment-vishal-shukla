package com.loadup.assessment.notification.exception;

public class MissingTenantHeaderException extends RuntimeException {
    public MissingTenantHeaderException(final String message) {
        super(message);
    }
}
