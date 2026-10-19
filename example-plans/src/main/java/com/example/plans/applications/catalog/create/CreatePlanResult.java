package com.example.plans.applications.catalog.create;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreatePlanResult(
        UUID id,
        String code,
        String name,
        boolean active,
        LocalDateTime createdAt) {
}
