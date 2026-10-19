package com.example.plans.persistence.adapter;

import com.example.plans.domain.model.PlanPrice;
import com.example.plans.domain.port.PlanPriceRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import com.example.plans.persistence.entity.PlanPriceEntity;
import com.example.plans.persistence.mapper.PlanPriceMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PlanPriceRepositoryAdapter implements PlanPriceRepository {

    private final EntityManager em;

    PlanPriceRepositoryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public PlanPrice save(PlanPrice price) {
        PlanPriceEntity entity;
        if (price.id() == null) {
            entity = PlanPriceMapper.newEntity(price);
            em.persist(entity);
        } else {
            entity = em.find(PlanPriceEntity.class, price.id());
            if (entity == null) {
                throw new ResourceNotFoundException("Plan price", price.id());
            }
            entity.changeAmount(price.amount());
            entity.changeActive(price.active());
        }
        em.flush();
        return PlanPriceMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanPrice> findById(UUID id) {
        return Optional.ofNullable(em.find(PlanPriceEntity.class, id)).map(PlanPriceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanPrice> findByPlanId(UUID planId) {
        return em.createQuery(
                        "select p from PlanPriceEntity p where p.planId = :planId order by p.currency, p.billingPeriod",
                        PlanPriceEntity.class)
                .setParameter("planId", planId)
                .getResultList()
                .stream()
                .map(PlanPriceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        PlanPriceEntity entity = em.find(PlanPriceEntity.class, id);
        if (entity == null) {
            throw new ResourceNotFoundException("Plan price", id);
        }
        em.remove(entity);
    }
}
