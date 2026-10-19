package com.example.plans.domain.port;

import com.example.plans.domain.model.Plan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    Plan save(Plan plan);

    Optional<Plan> findById(UUID id);

    Optional<Plan> findByCode(String code);

    List<Plan> findActive();
}
