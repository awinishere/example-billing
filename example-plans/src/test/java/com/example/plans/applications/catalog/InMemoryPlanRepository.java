package com.example.plans.applications.catalog;

import com.example.plans.domain.model.Plan;
import com.example.plans.domain.port.PlanRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;

public class InMemoryPlanRepository implements PlanRepository {

    private final Map<UUID, Plan> rows = new LinkedHashMap<>();
    private final List<String> codes = new ArrayList<>();

    @Override
    public Plan save(Plan plan) {
        if (plan.id() == null) {
            return insert(plan);
        }
        return update(plan);
    }

    private Plan insert(Plan plan) {
        if (codes.contains(plan.code())) {
            throw new DataIntegrityViolationException("uq_plan_code");
        }
        codes.add(plan.code());
        LocalDateTime now = LocalDateTime.now();
        Plan saved = new Plan(UUID.randomUUID(), plan.code(), plan.name(), plan.description(), plan.active(),
                now, now);
        rows.put(saved.id(), saved);
        return saved;
    }

    private Plan update(Plan plan) {
        Plan existing = rows.get(plan.id());
        if (existing == null) {
            throw new ResourceNotFoundException("Plan", plan.id());
        }
        Plan saved = new Plan(existing.id(), existing.code(), plan.name(), plan.description(), plan.active(),
                existing.createdAt(), LocalDateTime.now());
        rows.put(saved.id(), saved);
        return saved;
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return rows.values().stream().filter(plan -> plan.code().equals(code)).findFirst();
    }

    @Override
    public List<Plan> findActive() {
        return rows.values().stream().filter(Plan::active).toList();
    }
}
