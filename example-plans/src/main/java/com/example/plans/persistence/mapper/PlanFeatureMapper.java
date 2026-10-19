package com.example.plans.persistence.mapper;

import com.example.plans.domain.model.PlanFeature;
import com.example.plans.persistence.entity.PlanFeatureEntity;

public final class PlanFeatureMapper {

    private PlanFeatureMapper() {
    }

    public static PlanFeatureEntity newEntity(PlanFeature planFeature) {
        return new PlanFeatureEntity(planFeature.planId(), planFeature.featureId(),
                planFeature.included(), planFeature.quantity());
    }

    public static PlanFeature toDomain(PlanFeatureEntity entity) {
        return new PlanFeature(entity.getId(), entity.getPlanId(), entity.getFeatureId(),
                entity.isIncluded(), entity.getQuantity(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
