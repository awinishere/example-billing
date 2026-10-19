package com.example.plans.persistence.adapter;

import com.example.plans.domain.model.Feature;
import com.example.plans.domain.port.FeatureRepository;
import com.example.plans.extensions.exception.ResourceNotFoundException;
import com.example.plans.persistence.entity.FeatureEntity;
import com.example.plans.persistence.mapper.FeatureMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class FeatureRepositoryAdapter implements FeatureRepository {

    private final EntityManager em;

    FeatureRepositoryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Feature save(Feature feature) {
        FeatureEntity entity;
        if (feature.id() == null) {
            entity = FeatureMapper.newEntity(feature);
            em.persist(entity);
        } else {
            entity = em.find(FeatureEntity.class, feature.id());
            if (entity == null) {
                throw new ResourceNotFoundException("Feature", feature.id());
            }
            entity.modify(feature.name(), feature.defaultQuantity(), feature.active());
        }
        em.flush();
        return FeatureMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Feature> findByCode(String code) {
        return em.createQuery("select f from FeatureEntity f where f.code = :code", FeatureEntity.class)
                .setParameter("code", code)
                .getResultList()
                .stream()
                .findFirst()
                .map(FeatureMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feature> findActive() {
        return em.createQuery("select f from FeatureEntity f where f.active = true order by f.code", FeatureEntity.class)
                .getResultList()
                .stream()
                .map(FeatureMapper::toDomain)
                .toList();
    }
}
