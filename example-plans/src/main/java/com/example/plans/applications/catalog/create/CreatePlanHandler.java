package com.example.plans.applications.catalog.create;

import com.example.plans.domain.model.Plan;
import com.example.plans.domain.port.PlanRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class CreatePlanHandler {

    private final PlanRepository planRepository;

    @Transactional
    public CreatePlanResult execute(CreatePlanCommand command) {
        Plan saved = planRepository.save(Plan.newPlan(
                command.code(), command.name(), command.description()));
        return new CreatePlanResult(
                saved.id(),
                saved.code(),
                saved.name(),
                saved.active(),
                saved.createdAt()
        );
    }
}
