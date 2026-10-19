package com.example.plans.extensions.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PlansExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Conflicts with existing data",
                "another row already owns this identity");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
