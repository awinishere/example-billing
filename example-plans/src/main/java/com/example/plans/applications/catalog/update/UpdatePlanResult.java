package com.example.plans.applications.catalog.update;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdatePlanResult(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
