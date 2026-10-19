package com.example.plans.extensions.exception;

import java.util.UUID;

public class ResourceNotFoundException extends PlansException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " not found: " + id);
    }
}
