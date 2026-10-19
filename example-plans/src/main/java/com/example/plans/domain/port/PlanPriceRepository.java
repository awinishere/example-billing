package com.example.plans.domain.port;

import com.example.plans.domain.model.PlanPrice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanPriceRepository {

    PlanPrice save(PlanPrice price);

    Optional<PlanPrice> findById(UUID id);

    List<PlanPrice> findByPlanId(UUID planId);

    void deleteById(UUID id);
}
