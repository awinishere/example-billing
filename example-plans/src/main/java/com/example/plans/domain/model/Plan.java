package com.example.plans.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Plan(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public Plan {
        requireText(code, "code is required");
        requireText(name, "name is required");
    }

    public static Plan newPlan(String code, String name, String description) {
        return new Plan(
                null,
                code,
                name,
                description,
                true,
                null,
                null
        );
    }

    public Plan inactive() {
        return new Plan(id, code, name, description, false, createdAt, updatedAt);
    }

    private static void requireText(String value, String reason) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(reason);
        }
    }
}
