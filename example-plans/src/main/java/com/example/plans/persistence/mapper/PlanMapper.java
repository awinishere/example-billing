package com.example.plans.persistence.mapper;

import com.example.plans.domain.model.Plan;
import com.example.plans.persistence.entity.PlanEntity;

public final class PlanMapper {

    private PlanMapper() {
    }

    public static PlanEntity newEntity(Plan plan) {
        return new PlanEntity(plan.code(), plan.name(), plan.description(), plan.active());
    }

    public static Plan toDomain(PlanEntity entity) {
        return new Plan(entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
