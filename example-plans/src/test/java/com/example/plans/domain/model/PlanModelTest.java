package com.example.plans.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.plans.extensions.exception.PlansException;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import com.example.plans.extensions.types.BillingPeriod;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanModelTest {

    @Test
    void rejectsBlankPlanCode() {
        assertThrows(IllegalArgumentException.class, () -> Plan.newPlan(" ", "Starter", "description"));
        assertThrows(IllegalArgumentException.class, () -> Plan.newPlan("starter", null, "description"));
    }

    @Test
    void rejectsPriceWithoutPlanOrWithNegativeAmount() {
        UUID planId = UUID.randomUUID();
        Currency idr = Currency.getInstance("IDR");

        assertThrows(IllegalArgumentException.class,
                () -> PlanPrice.newPrice(null, BigDecimal.TEN, idr, BillingPeriod.MONTHLY));
        assertThrows(IllegalArgumentException.class,
                () -> PlanPrice.newPrice(planId, BigDecimal.valueOf(-1), idr, BillingPeriod.MONTHLY));
        assertThrows(IllegalArgumentException.class,
                () -> PlanPrice.newPrice(planId, BigDecimal.TEN, null, BillingPeriod.MONTHLY));
        assertThrows(IllegalArgumentException.class,
                () -> PlanPrice.newPrice(planId, BigDecimal.TEN, idr, null));
    }

    @Test
    void rejectsEntitlementWithoutReferencesOrWithNegativeQuantity() {
        UUID planId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> PlanFeature.newPlanFeature(planId, null, true, 3));
        assertThrows(IllegalArgumentException.class,
                () -> PlanFeature.newPlanFeature(planId, featureId, true, -5));
    }

    @Test
    void rejectsFeatureWithoutCode() {
        assertThrows(IllegalArgumentException.class, () -> Feature.newFeature("", "Device seats", 3));
    }

    @Test
    void invariantFailuresCarryAReadableReason() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Plan.newPlan("ok", "  ", "blank name is invalid"));
        assertEquals("name is required", failure.getMessage());
    }

    @Test
    void theOneModuleExceptionNamesResourceAndId() {
        UUID id = UUID.randomUUID();

        PlansException failure = assertThrows(PlansException.class,
                () -> {
                    throw new ResourceNotFoundException("Plan price", id);
                });
        assertEquals("Plan price not found: " + id, failure.getMessage());
    }
}
