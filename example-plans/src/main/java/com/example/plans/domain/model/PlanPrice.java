package com.example.plans.domain.model;

import com.example.plans.extensions.types.BillingPeriod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

public record PlanPrice(
        UUID id,
        UUID planId,
        BigDecimal amount,
        Currency currency,
        BillingPeriod billingPeriod,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public PlanPrice {
        if (planId == null) {
            throw new IllegalArgumentException("planId is required: a price must belong to a plan");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (billingPeriod == null) {
            throw new IllegalArgumentException("billingPeriod is required");
        }
    }

    public static PlanPrice newPrice(
            UUID planId,
            BigDecimal amount,
            Currency currency,
            BillingPeriod billingPeriod
    ) {
        return new PlanPrice(
                null,
                planId,
                amount,
                currency,
                billingPeriod,
                true,
                null,
                null
        );
    }
}
