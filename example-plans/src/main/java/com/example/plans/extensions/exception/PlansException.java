package com.example.plans.extensions.exception;

public abstract class PlansException extends RuntimeException {

    protected PlansException(String message) {
        super(message);
    }
}
