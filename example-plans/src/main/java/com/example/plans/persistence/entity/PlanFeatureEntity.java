package com.example.plans.persistence.entity;

import com.example.shared.kernel.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(
        name = "plan_feature",
        schema = "plans",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_feature", columnNames = {"plan_id", "feature_id"}))
public class PlanFeatureEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "feature_id", nullable = false, updatable = false)
    private UUID featureId;

    @Column(name = "included", nullable = false)
    private boolean included;

    @Column(name = "quantity")
    private Integer quantity;

    public PlanFeatureEntity(UUID planId, UUID featureId, boolean included, Integer quantity) {
        this.planId = planId;
        this.featureId = featureId;
        this.included = included;
        this.quantity = quantity;
    }

    public void changeEntitlement(boolean included, Integer quantity) {
        this.included = included;
        this.quantity = quantity;
    }
}
