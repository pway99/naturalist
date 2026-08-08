package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class InsectFeatureTestEntitySource extends TestEntitySource<InsectFeatureId, InsectFeature> {

    public InsectFeatureTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-features.json");
    }

    @Override
    protected List<UniqueConstraint<InsectFeature>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "value";
                    }

                    @Override
                    public Function<InsectFeature, ?> valueFunction() {
                        return InsectFeature::value;
                    }
                });
    }

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on
     * {@code value}: the existing row's id is retained, the caller-supplied
     * (typically freshly-minted, per-identification) id is discarded. This is
     * what lets a re-identification that reproduces an already-catalogued
     * feature value reuse the existing row instead of tripping the constraint —
     * see {@code InsectCatalogIdentificationTransaction}.
     */
    @Override
    protected InsectFeature withKey(InsectFeature feature, InsectFeatureId key) {
        return new InsectFeature(key, feature.value());
    }
}
