package com.example.plans.applications.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.example.plans.applications.catalog.create.CreatePlanCommand;
import com.example.plans.applications.catalog.create.CreatePlanHandler;
import com.example.plans.applications.catalog.create.CreatePlanResult;
import org.junit.jupiter.api.Test;

class CreatePlanHandlerTest {

    @Test
    void executeStoresPlanThroughPortAndReturnsGeneratedIdentity() {
        InMemoryPlanRepository repository = new InMemoryPlanRepository();
        CreatePlanHandler handler = new CreatePlanHandler(repository);

        CreatePlanResult result = handler.execute(new CreatePlanCommand("starter", "Starter", "for small teams"));

        assertNotNull(result.id(), "the caller must get the generated id back, not only the entity");
        assertEquals("starter", result.code());
        assertEquals("Starter", result.name());
        assertTrue(result.active());
        assertNotNull(result.createdAt());
        assertEquals(result.id(), repository.findById(result.id()).orElseThrow().id());
    }

    @Test
    void blankCodeIsRejectedByTheDomainRecordNotByTheHandler() {
        CreatePlanHandler handler = new CreatePlanHandler(new InMemoryPlanRepository());

        assertThrows(IllegalArgumentException.class,
                () -> handler.execute(new CreatePlanCommand(" ", "Starter", "description")));
    }

    @Test
    void nullNameIsRejectedByTheDomainRecordNotByTheHandler() {
        CreatePlanHandler handler = new CreatePlanHandler(new InMemoryPlanRepository());

        assertThrows(IllegalArgumentException.class,
                () -> handler.execute(new CreatePlanCommand("starter", null, "description")));
    }

    @Test
    void duplicateCodeSurfacesAsSpringConflictFromThePort() {
        CreatePlanHandler handler = new CreatePlanHandler(new InMemoryPlanRepository());
        handler.execute(new CreatePlanCommand("free", "Free", "first"));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> handler.execute(new CreatePlanCommand("free", "Free again", "second")));
    }

    @Test
    void resultCarriesNoPriceBecausePriceBelongsToItsOwnAggregate() {
        assertTrue(java.util.Arrays.stream(CreatePlanResult.class.getRecordComponents())
                        .noneMatch(component -> component.getName().equals("price")),
                "CreatePlanResult must not hold a price: amount, currency and period live in PlanPrice");
        assertEquals(UUID.class, RecordComponentType.of(CreatePlanResult.class, "id"));
    }

    private static final class RecordComponentType {
        static Class<?> of(Class<?> recordType, String componentName) {
            return java.util.Arrays.stream(recordType.getRecordComponents())
                    .filter(component -> component.getName().equals(componentName))
                    .findFirst()
                    .orElseThrow()
                    .getType();
        }
    }
}
