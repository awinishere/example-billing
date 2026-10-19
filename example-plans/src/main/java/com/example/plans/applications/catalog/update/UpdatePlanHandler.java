package com.example.plans.applications.catalog.update;

import com.example.plans.domain.model.Plan;
import com.example.plans.domain.port.PlanRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UpdatePlanHandler {

    private final PlanRepository planRepository;

    @Transactional
    public UpdatePlanResult execute(UpdatePlanCommand command) {
        Plan current = planRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", command.id()));

        Plan saved = planRepository.save(new Plan(
                current.id(),
                current.code(),
                command.name(),
                command.description(),
                current.active(),
                null,
                null
        ));

        return new UpdatePlanResult(
                saved.id(),
                saved.code(),
                saved.name(),
                saved.description(),
                saved.active(),
                saved.createdAt(),
                saved.updatedAt()
        );
    }
}
