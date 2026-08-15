package com.naturalist.garden;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.PlantName;

import java.util.List;

/**
 * Behavioral contract for {@link CropTypeRepository}. Inherits the {@link EntityRepositoryTest}
 * cases (ADR-002); supplies garden identity constants and entity construction. No domain-specific
 * query methods — nothing points at a crop type except by name.
 */
interface CropTypeEntityRepositoryTest extends EntityRepositoryTest<CropTypeName, CropType> {

    @Override
    CropTypeRepository repository();

    @Override
    default TestEntitySource<CropTypeName, CropType> source() {
        return db.getNamed(CropTypeTestEntitySource.class);
    }

    @Override
    default CropTypeName notFoundName() {
        return TestGardenIdentifiers.CropTypes.notFound;
    }

    @Override
    default List<CropTypeName> knownEntityNames() {
        return List.of(
                TestGardenIdentifiers.CropTypes.tomato,
                TestGardenIdentifiers.CropTypes.lettuce);
    }

    @Override
    default CropType newEntity() {
        return new CropType(CropTypeName.of(RandomValue.string()), PlantName.of(RandomValue.string()));
    }

    /** A type with no botanical identification — the shape lettuce actually has. */
    @Override
    default CropType ghostEntity() {
        return new CropType(CropTypeName.of(RandomValue.string()), null);
    }

    @Override
    default CropType modifiedEntity(CropType original) {
        return new CropType(original.name(), PlantName.of(RandomValue.string()));
    }
}
