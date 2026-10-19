package com.example.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Feature(
        UUID id,
        String code,
        String name,
        Integer defaultQuantity,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public Feature {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    public static Feature newFeature(String code, String name, Integer defaultQuantity) {
        return new Feature(null, code, name, defaultQuantity, true, null, null);
    }
}
