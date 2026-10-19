package com.example.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PlanFeature(
        UUID id,
        UUID planId,
        UUID featureId,
        boolean included,
        Integer quantity,
        Instant createdAt,
        Instant updatedAt) {

    public PlanFeature {
        if (planId == null || featureId == null) {
            throw new IllegalArgumentException("planId and featureId are required");
        }
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative: " + quantity);
        }
    }

    public static PlanFeature newPlanFeature(UUID planId, UUID featureId, boolean included, Integer quantity) {
        return new PlanFeature(null, planId, featureId, included, quantity, null, null);
    }
}
