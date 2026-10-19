package com.example.plans.persistence.mapper;

import com.example.plans.domain.model.PlanPrice;
import com.example.plans.persistence.entity.PlanPriceEntity;

public final class PlanPriceMapper {

    private PlanPriceMapper() {
    }

    public static PlanPriceEntity newEntity(PlanPrice price) {
        return new PlanPriceEntity(price.planId(), price.amount(), price.currency(),
                price.billingPeriod(), price.active());
    }

    public static PlanPrice toDomain(PlanPriceEntity entity) {
        return new PlanPrice(entity.getId(), entity.getPlanId(), entity.getAmount(), entity.getCurrency(),
                entity.getBillingPeriod(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
