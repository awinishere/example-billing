package com.example.plans.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.plans.IntegrationTest;
import com.example.plans.extensions.types.BillingPeriod;
import com.example.plans.persistence.entity.FeatureEntity;
import com.example.plans.persistence.entity.PlanEntity;
import com.example.plans.persistence.entity.PlanFeatureEntity;
import com.example.plans.persistence.entity.PlanPriceEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class PlanPersistenceTest extends IntegrationTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID insertPlan(String code) {
        UUID[] id = new UUID[1];
        tx.executeWithoutResult(status -> {
            PlanEntity plan = new PlanEntity(code, "Plan " + code, "description", true);
            em.persist(plan);
            id[0] = plan.getId();
        });
        return id[0];
    }

    @Test
    void persistsPlanPriceAndFeatureWithKernelAudit() {
        UUID planId = insertPlan("ultimate");

        tx.executeWithoutResult(status -> {
            em.persist(new PlanPriceEntity(planId, new BigDecimal("150000.0000"),
                    Currency.getInstance("IDR"), BillingPeriod.MONTHLY, true));
            em.persist(new PlanPriceEntity(planId, new BigDecimal("1500000.0000"),
                    Currency.getInstance("IDR"), BillingPeriod.YEARLY, true));
        });

        UUID featureId = tx.execute(status -> {
            FeatureEntity feature = new FeatureEntity("AI_CREDITS", "AI credits", 100, true);
            em.persist(feature);
            return feature.getId();
        });

        tx.executeWithoutResult(status -> em.persist(
                new PlanFeatureEntity(planId, featureId, true, 100)));

        PlanPriceEntity price = tx.execute(status -> em.createQuery(
                        "select p from PlanPriceEntity p where p.planId = :planId and p.billingPeriod = :period",
                        PlanPriceEntity.class)
                .setParameter("planId", planId)
                .setParameter("period", BillingPeriod.MONTHLY)
                .getSingleResult());
        assertNotNull(price.getId());
        assertNotNull(price.getCreatedAt(), "created_at must come from BaseEntity (shared kernel)");
        assertNotNull(price.getUpdatedAt());

        PlanEntity plan = tx.execute(status -> em.find(PlanEntity.class, planId));
        assertEquals("ultimate", plan.getCode());
        assertNotNull(plan.getCreatedAt());
    }

    @Test
    void storesBillingPeriodAsVarcharNotNativeEnum() {
        String dataType = jdbcTemplate.queryForObject(
                "select data_type from information_schema.columns "
                        + "where table_schema = 'plans' and table_name = 'plan_price' "
                        + "and column_name = 'billing_period'",
                String.class);
        assertEquals("character varying", dataType);
    }

    @Test
    void rejectsDuplicatePlanCode() {
        insertPlan("free");
        assertThrows(DataIntegrityViolationException.class, () -> insertPlan("free"));
    }

    @Test
    void rejectsTwoPricesWithSamePlanCurrencyAndPeriod() {
        UUID planId = insertPlan("pro");
        tx.executeWithoutResult(status -> em.persist(new PlanPriceEntity(planId,
                new BigDecimal("99000.0000"), Currency.getInstance("IDR"), BillingPeriod.MONTHLY, true)));

        assertThrows(DataIntegrityViolationException.class, () -> tx.executeWithoutResult(status -> {
            em.clear();
            em.persist(new PlanPriceEntity(planId, new BigDecimal("120000.0000"),
                    Currency.getInstance("IDR"), BillingPeriod.MONTHLY, true));
        }));
    }

    @Test
    void rejectsPriceWithoutParentPlan() {
        assertThrows(DataIntegrityViolationException.class, () -> tx.executeWithoutResult(status ->
                em.persist(new PlanPriceEntity(UUID.randomUUID(), new BigDecimal("1.0000"),
                        Currency.getInstance("USD"), BillingPeriod.ONE_TIME, true))));
    }
}
