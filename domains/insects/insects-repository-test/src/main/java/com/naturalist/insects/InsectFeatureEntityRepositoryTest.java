package com.naturalist.insects;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link InsectRepository.FeatureRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002). Supplies
 * {@code InsectFeature}-specific identity constants and entity construction.
 */
interface InsectFeatureEntityRepositoryTest
        extends EntityRepositoryTest<InsectFeatureId, InsectFeature> {

    @Override
    InsectRepository.FeatureRepository repository();

    @Override
    default TestEntitySource<InsectFeatureId, InsectFeature> source() {
        return db.getNamed(InsectFeatureTestEntitySource.class);
    }

    @Override
    default InsectFeatureId notFoundName() {
        return TestInsectsIdentifiers.InsectFeature.NotFound.id;
    }

    @Override
    default List<InsectFeatureId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFeature.CompleteMetamorphosis.id,
                TestInsectsIdentifiers.InsectFeature.IncompleteMetamorphosis.id,
                TestInsectsIdentifiers.InsectFeature.ScaledWings.id);
    }

    @Override
    default InsectFeature newEntity() {
        return new InsectFeature(
                InsectFeatureId.create(),
                "net-veined wings");
    }

    @Override
    default InsectFeature ghostEntity() {
        return new InsectFeature(
                InsectFeatureId.create(),
                "bioluminescent abdomen");
    }

    @Override
    default InsectFeature modifiedEntity(InsectFeature original) {
        return new InsectFeature(
                original.id(),
                "modified-" + original.value());
    }
}
