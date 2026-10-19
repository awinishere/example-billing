package com.example.plans.applications.catalog.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.plans.applications.catalog.InMemoryPlanRepository;
import com.example.plans.domain.model.Plan;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdatePlanHandlerTest {

    private final InMemoryPlanRepository planRepository = new InMemoryPlanRepository();
    private final UpdatePlanHandler handler = new UpdatePlanHandler(planRepository);

    @Test
    void executeWritesOnlyNameAndDescription() {
        UUID id = planRepository.save(Plan.newPlan("pro", "Pro", "old description")).id();

        UpdatePlanResult result = handler.execute(new UpdatePlanCommand(id, "Pro Renamed", "new description"));

        assertEquals("Pro Renamed", result.name());
        assertEquals("new description", result.description());
        Plan stored = planRepository.findById(id).orElseThrow();
        assertEquals("Pro Renamed", stored.name());
        assertEquals("new description", stored.description());
    }

    @Test
    void executeFreezesCodeBecauseTheCommandCarriesNoWriteRightOnIt() {
        UUID id = planRepository.save(Plan.newPlan("pro", "Pro", "old description")).id();

        UpdatePlanResult result = handler.execute(new UpdatePlanCommand(id, "Pro Renamed", "new description"));

        assertEquals("pro", result.code());
        assertEquals("pro", planRepository.findById(id).orElseThrow().code());
    }

    @Test
    void executeKeepsTheStoredStatusSoAClientCannotFlipItByUpdatingContent() {
        UUID id = planRepository.save(new Plan(null, "legacy", "Legacy", "old", false, null, null)).id();

        UpdatePlanResult result = handler.execute(new UpdatePlanCommand(id, "Legacy Renamed", "new"));

        assertFalse(result.active(), "updating content must not reactivate a deactivated plan");
    }

    @Test
    void executeKeepsCreatedAtAndRefreshesUpdatedAt() {
        UUID id = planRepository.save(Plan.newPlan("pro", "Pro", "old description")).id();
        Plan before = planRepository.findById(id).orElseThrow();

        UpdatePlanResult result = handler.execute(new UpdatePlanCommand(id, "Pro Renamed", "new description"));

        assertEquals(before.createdAt(), result.createdAt());
        assertFalse(result.updatedAt().isBefore(before.updatedAt()));
    }

    @Test
    void executeRaisesNotFoundForUnknownId() {
        UUID missing = UUID.randomUUID();

        ResourceNotFoundException failure = assertThrows(ResourceNotFoundException.class,
                () -> handler.execute(new UpdatePlanCommand(missing, "Ghost", "description")));

        assertTrue(failure.getMessage().contains("Plan not found: " + missing), failure.getMessage());
    }

    @Test
    void blankNameIsRejectedByTheDomainRecordNotByTheHandler() {
        UUID id = planRepository.save(Plan.newPlan("pro", "Pro", "old description")).id();

        assertThrows(IllegalArgumentException.class,
                () -> handler.execute(new UpdatePlanCommand(id, " ", "new description")));
    }
}
