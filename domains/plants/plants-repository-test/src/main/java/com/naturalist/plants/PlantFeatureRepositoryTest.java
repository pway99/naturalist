package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link PlantRepository.FeatureRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002). Supplies
 * {@code PlantFeature}-specific identity constants and entity construction. Mirrors
 * {@code InsectFeatureEntityRepositoryTest}.
 */
interface PlantFeatureRepositoryTest
        extends EntityRepositoryTest<PlantFeatureId, PlantFeature> {

    @Override
    PlantRepository.FeatureRepository repository();

    @Override
    default TestEntitySource<PlantFeatureId, PlantFeature> source() {
        return db.getNamed(PlantFeatureTestEntitySource.class);
    }

    @Override
    default PlantFeatureId notFoundName() {
        return TestPlantsIdentifiers.PlantFeatures.NotFound.id;
    }

    @Override
    default List<PlantFeatureId> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.PlantFeatures.RayFlorets,
                TestPlantsIdentifiers.PlantFeatures.OppositeLeaves);
    }

    @Override
    default PlantFeature newEntity() {
        return new PlantFeature(
                PlantFeatureId.create(),
                "new feature " + RandomValue.string());
    }

    @Override
    default PlantFeature ghostEntity() {
        return new PlantFeature(
                PlantFeatureId.create(),
                "ghost feature " + RandomValue.string());
    }

    @Override
    default PlantFeature modifiedEntity(PlantFeature original) {
        return new PlantFeature(
                original.id(),
                "modified-" + RandomValue.string());
    }
}
