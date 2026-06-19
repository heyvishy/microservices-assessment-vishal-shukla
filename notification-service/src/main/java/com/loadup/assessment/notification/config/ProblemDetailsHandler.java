package com.loadup.assessment.notification.config;

import jakarta.persistence.EntityNotFoundException;
import com.loadup.assessment.notification.exception.MissingTenantContextException;
import com.loadup.assessment.notification.exception.MissingTenantHeaderException;
import com.loadup.assessment.notification.exception.UnknownTenantException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemDetailsHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail notFound(EntityNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MissingTenantHeaderException.class)
    ProblemDetail badRequest(MissingTenantHeaderException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingTenantContextException.class)
    ProblemDetail missingTenantContext(MissingTenantContextException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnknownTenantException.class)
    ProblemDetail unknownTenant(UnknownTenantException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ProblemDetail problem(final HttpStatus status, final String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        return detail;
    }
}
