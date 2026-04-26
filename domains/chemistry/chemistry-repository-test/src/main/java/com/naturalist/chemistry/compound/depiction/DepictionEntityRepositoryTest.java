package com.naturalist.chemistry.compound.depiction;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link DepictionRepository.DepictionEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies CompoundDepiction-specific identity constants and entity construction.
 */
interface DepictionEntityRepositoryTest
        extends EntityRepositoryTest<DepictionId, CompoundDepiction> {

    @Override
    DepictionRepository.DepictionEntityRepository repository();

    @Override
    default TestEntitySource<DepictionId, CompoundDepiction> source() {
        return db.getNamed(CompoundDepictionTestEntitySource.class);
    }

    @Override
    default DepictionId notFoundName() {
        return TestChemistryIdentifiers.Compounds.NotFound.depictionName;
    }

    @Override
    default List<DepictionId> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.depictionName,
                TestChemistryIdentifiers.Compounds.FormicAcid.depictionName);
    }

    @Override
    default CompoundDepiction newEntity() {
        return new CompoundDepiction(
                DepictionId.create(),
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                RandomValue.string());
    }

    @Override
    default CompoundDepiction ghostEntity() {
        return new CompoundDepiction(
                DepictionId.create(),
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                RandomValue.string());
    }

    @Override
    default CompoundDepiction modifiedEntity(CompoundDepiction original) {
        return new CompoundDepiction(
                original.name(),
                original.compoundName(),
                RandomValue.string(),
                RandomValue.string());
    }
}
