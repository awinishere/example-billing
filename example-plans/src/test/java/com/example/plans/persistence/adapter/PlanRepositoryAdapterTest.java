package com.example.plans.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.plans.IntegrationTest;
import com.example.plans.domain.model.Feature;
import com.example.plans.domain.model.Plan;
import com.example.plans.domain.model.PlanFeature;
import com.example.plans.domain.model.PlanPrice;
import com.example.plans.domain.port.FeatureRepository;
import com.example.plans.domain.port.PlanFeatureRepository;
import com.example.plans.domain.port.PlanPriceRepository;
import com.example.plans.domain.port.PlanRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import com.example.plans.extensions.types.BillingPeriod;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class PlanRepositoryAdapterTest extends IntegrationTest {

    @Autowired
    private PlanRepository plans;

    @Autowired
    private PlanPriceRepository prices;

    @Autowired
    private FeatureRepository features;

    @Autowired
    private PlanFeatureRepository planFeatures;

    @Test
    void saveReturnsGeneratedIdAndKernelAudit() {
        Plan saved = plans.save(Plan.newPlan("starter", "Starter", "for small teams"));

        assertNotNull(saved.id(), "the record must carry the id, not just the entity");
        assertNotNull(saved.createdAt(), "audit columns come from BaseEntity in the shared kernel");
        assertNotNull(saved.updatedAt());

        Plan loaded = plans.findByCode("starter").orElseThrow();
        assertEquals(saved.id(), loaded.id());
        assertTrue(loaded.active());
    }

    @Test
    void saveWithIdUpdatesContentButNeverCode() {
        Plan saved = plans.save(Plan.newPlan("premium", "Premium", "old"));

        Plan changed = plans.save(new Plan(saved.id(), saved.code(), "Premium Renamed", "new description",
                false, saved.createdAt(), saved.updatedAt()));

        assertEquals("Premium Renamed", changed.name());
        assertEquals("new description", changed.description());
        assertFalse(changed.active());
        assertEquals("premium", changed.code(), "code is identity and must not change through save");
    }

    @Test
    void saveWithUnknownIdRaisesNotFound() {
        Plan ghost = new Plan(UUID.randomUUID(), "ghost", "Ghost", "not in db", true, null, null);

        ResourceNotFoundException failure = assertThrows(ResourceNotFoundException.class, () -> plans.save(ghost));
        assertEquals("Plan not found: " + ghost.id(), failure.getMessage());
    }

    @Test
    void savesMultiplePricesPerPlanThroughPort() {
        Plan plan = plans.save(Plan.newPlan("catalog", "Catalog", "several prices"));
        prices.save(PlanPrice.newPrice(plan.id(), new BigDecimal("150000.0000"),
                Currency.getInstance("IDR"), BillingPeriod.MONTHLY));
        prices.save(PlanPrice.newPrice(plan.id(), new BigDecimal("1500000.0000"),
                Currency.getInstance("IDR"), BillingPeriod.YEARLY));

        List<PlanPrice> found = prices.findByPlanId(plan.id());
        assertEquals(2, found.size());
        assertEquals(BillingPeriod.MONTHLY, found.get(0).billingPeriod());
        assertNotNull(found.get(0).createdAt());
    }

    @Test
    void storesFeatureEntitlementThroughPort() {
        Feature devices = features.save(Feature.newFeature("DEVICES", "Device seats", 3));
        Plan plan = plans.save(Plan.newPlan("entitlement", "Entitlement", "feature check"));
        planFeatures.save(PlanFeature.newPlanFeature(plan.id(), devices.id(), true, 3));

        List<PlanFeature> granted = planFeatures.findByPlanId(plan.id());
        assertEquals(1, granted.size());
        assertEquals(Integer.valueOf(3), granted.get(0).quantity());
        assertNotNull(granted.get(0).createdAt());
        assertTrue(features.findByCode("DEVICES").orElseThrow().active());
    }

    @Test
    void rejectsDuplicatePlanCodeThroughAdapter() {
        plans.save(Plan.newPlan("duplicate", "First", "original"));
        assertThrows(DataIntegrityViolationException.class,
                () -> plans.save(Plan.newPlan("duplicate", "Second", "not a duplicate by intent")));
    }

    @Test
    void rejectsPriceWithoutParentPlanThroughAdapter() {
        assertThrows(DataIntegrityViolationException.class, () -> prices.save(
                PlanPrice.newPrice(UUID.randomUUID(), new BigDecimal("10.0000"),
                        Currency.getInstance("USD"), BillingPeriod.ONE_TIME)));
    }

    @Test
    void modelRecordsNeverReferenceJpaTypes() {
        for (Class<?> type : List.of(Plan.class, PlanPrice.class, PlanFeature.class, Feature.class)) {
            assertTrue(type.isRecord(), type + " must be a record");
            for (var component : type.getRecordComponents()) {
                String packageName = component.getType().getPackageName();
                assertFalse(packageName.startsWith("jakarta.persistence")
                                || packageName.startsWith("org.hibernate"),
                        type.getSimpleName() + "." + component.getName() + " leaks a JPA type into the model");
            }
        }
    }
}
