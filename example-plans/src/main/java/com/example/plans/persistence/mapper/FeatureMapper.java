package com.example.plans.persistence.mapper;

import com.example.plans.domain.model.Feature;
import com.example.plans.persistence.entity.FeatureEntity;

public final class FeatureMapper {

    private FeatureMapper() {
    }

    public static FeatureEntity newEntity(Feature feature) {
        return new FeatureEntity(feature.code(), feature.name(), feature.defaultQuantity(), feature.active());
    }

    public static Feature toDomain(FeatureEntity entity) {
        return new Feature(entity.getId(), entity.getCode(), entity.getName(), entity.getDefaultQuantity(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
