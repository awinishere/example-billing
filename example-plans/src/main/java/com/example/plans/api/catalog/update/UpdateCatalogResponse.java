package com.example.plans.api.catalog.update;

import com.example.plans.applications.catalog.update.UpdatePlanResult;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCatalogResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UpdateCatalogResponse from(UpdatePlanResult result) {
        return new UpdateCatalogResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
