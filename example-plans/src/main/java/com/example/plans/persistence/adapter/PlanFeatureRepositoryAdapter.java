package com.example.plans.persistence.adapter;

import com.example.plans.domain.model.PlanFeature;
import com.example.plans.domain.port.PlanFeatureRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import com.example.plans.persistence.entity.PlanFeatureEntity;
import com.example.plans.persistence.mapper.PlanFeatureMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PlanFeatureRepositoryAdapter implements PlanFeatureRepository {

    private final EntityManager em;

    PlanFeatureRepositoryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public PlanFeature save(PlanFeature planFeature) {
        PlanFeatureEntity entity;
        if (planFeature.id() == null) {
            entity = PlanFeatureMapper.newEntity(planFeature);
            em.persist(entity);
        } else {
            entity = em.find(PlanFeatureEntity.class, planFeature.id());
            if (entity == null) {
                throw new ResourceNotFoundException("Plan feature", planFeature.id());
            }
            entity.changeEntitlement(planFeature.included(), planFeature.quantity());
        }
        em.flush();
        return PlanFeatureMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanFeature> findById(UUID id) {
        return Optional.ofNullable(em.find(PlanFeatureEntity.class, id)).map(PlanFeatureMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanFeature> findByPlanId(UUID planId) {
        return em.createQuery(
                        "select f from PlanFeatureEntity f where f.planId = :planId order by f.featureId",
                        PlanFeatureEntity.class)
                .setParameter("planId", planId)
                .getResultList()
                .stream()
                .map(PlanFeatureMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        PlanFeatureEntity entity = em.find(PlanFeatureEntity.class, id);
        if (entity == null) {
            throw new ResourceNotFoundException("Plan feature", id);
        }
        em.remove(entity);
    }
}
