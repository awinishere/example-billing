package com.example.plans.persistence.entity;

import com.example.shared.kernel.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "feature", schema = "plans")
public class FeatureEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "default_quantity")
    private Integer defaultQuantity;

    @Column(nullable = false)
    private boolean active;

    public FeatureEntity(String code, String name, Integer defaultQuantity, boolean active) {
        this.code = code;
        this.name = name;
        this.defaultQuantity = defaultQuantity;
        this.active = active;
    }

    public void modify(String name, Integer defaultQuantity, boolean active) {
        this.name = name;
        this.defaultQuantity = defaultQuantity;
        this.active = active;
    }
}
