package com.example.plans.domain.port;

import com.example.plans.domain.model.Feature;
import java.util.List;
import java.util.Optional;

public interface FeatureRepository {

    Feature save(Feature feature);

    Optional<Feature> findByCode(String code);

    List<Feature> findActive();
}
