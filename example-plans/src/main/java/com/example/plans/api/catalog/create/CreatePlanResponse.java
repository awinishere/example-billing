package com.example.plans.api.catalog.create;

import com.example.plans.applications.catalog.create.CreatePlanResult;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreatePlanResponse(
        UUID id,
        String code,
        String name,
        boolean active,
        LocalDateTime createdAt) {

    public static CreatePlanResponse from(CreatePlanResult result) {
        return new CreatePlanResponse(
                result.id(),
                result.code(),
                result.name(),
                result.active(),
                result.createdAt()
        );
    }
}
