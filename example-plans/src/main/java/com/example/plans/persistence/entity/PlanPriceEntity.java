package com.example.plans.persistence.entity;

import com.example.plans.extensions.types.BillingPeriod;
import com.example.shared.kernel.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(
        name = "plan_price",
        schema = "plans",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_price", columnNames = {"plan_id", "currency", "billing_period"}))
public class PlanPriceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod;

    @Column(name = "active", nullable = false)
    private boolean active;

    public PlanPriceEntity(UUID planId, BigDecimal amount, Currency currency, BillingPeriod billingPeriod, boolean active) {
        this.planId = planId;
        this.amount = amount;
        this.currency = currency;
        this.billingPeriod = billingPeriod;
        this.active = active;
    }

    public void changeAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
