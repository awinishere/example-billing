package com.example.plans.domain.port;

import com.example.plans.domain.model.PlanFeature;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanFeatureRepository {

    PlanFeature save(PlanFeature planFeature);

    Optional<PlanFeature> findById(UUID id);

    List<PlanFeature> findByPlanId(UUID planId);

    void deleteById(UUID id);
}
