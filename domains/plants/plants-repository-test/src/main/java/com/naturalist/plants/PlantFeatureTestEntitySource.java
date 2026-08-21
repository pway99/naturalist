package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * Mirrors {@code InsectFeatureTestEntitySource} — {@link PlantFeature} carries surrogate
 * ({@link PlantFeatureId}) identity, so this extends {@link TestEntitySource} directly
 * rather than the natural-key {@code NamedTestEntitySource}.
 */
public class PlantFeatureTestEntitySource extends TestEntitySource<PlantFeatureId, PlantFeature> {

    public PlantFeatureTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-features.json");
    }

    @Override
    protected List<UniqueConstraint<PlantFeature>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "value";
                    }

                    @Override
                    public Function<PlantFeature, ?> valueFunction() {
                        return PlantFeature::value;
                    }
                });
    }

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on {@code value}: the
     * existing row's id is retained, the caller-supplied (typically freshly-minted, per
     * identification) id is discarded. Mirrors {@code InsectFeatureTestEntitySource}.
     */
    @Override
    protected PlantFeature withKey(PlantFeature feature, PlantFeatureId key) {
        return new PlantFeature(key, feature.value());
    }
}
