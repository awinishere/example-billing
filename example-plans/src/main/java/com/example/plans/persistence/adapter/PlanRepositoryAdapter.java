package com.example.plans.persistence.adapter;

import com.example.plans.domain.model.Plan;
import com.example.plans.domain.port.PlanRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import com.example.plans.persistence.entity.PlanEntity;
import com.example.plans.persistence.mapper.PlanMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PlanRepositoryAdapter implements PlanRepository {

    private final EntityManager em;

    PlanRepositoryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Plan save(Plan plan) {
        PlanEntity entity;
        if (plan.id() == null) {
            entity = PlanMapper.newEntity(plan);
            em.persist(entity);
        } else {
            entity = em.find(PlanEntity.class, plan.id());
            if (entity == null) {
                throw new ResourceNotFoundException("Plan", plan.id());
            }
            entity.modify(plan.name(), plan.description(), plan.active());
        }
        em.flush();
        return PlanMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> findById(UUID id) {
        return Optional.ofNullable(em.find(PlanEntity.class, id)).map(PlanMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> findByCode(String code) {
        return em.createQuery("select p from PlanEntity p where p.code = :code", PlanEntity.class)
                .setParameter("code", code)
                .getResultList()
                .stream()
                .findFirst()
                .map(PlanMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> findActive() {
        return em.createQuery("select p from PlanEntity p where p.active = true order by p.code", PlanEntity.class)
                .getResultList()
                .stream()
                .map(PlanMapper::toDomain)
                .toList();
    }
}
