package com.naturalist.chemistry.compound.depiction;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // =========================================================================
    // getByCompoundName — FK-unique lookup
    // =========================================================================

    @Test
    default void getByCompoundName_nullArgument() {
        assertThatThrownBy(() -> repository().getByCompoundName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("compoundName");
    }

    @Test
    default void getByCompoundName_unknownCompound_returnsEmpty() {
        assertThat(repository().getByCompoundName(TestChemistryIdentifiers.Compounds.NotFound.name))
                .isEmpty();
    }

    @Test
    default void getByCompoundName_knownCompound_returnsDepiction() {
        CompoundName knownCompound = TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.name;
        DepictionId expectedId = TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.depictionName;
        CompoundDepiction expected = source().getByName(expectedId).orElseThrow();

        Optional<CompoundDepiction> result = repository().getByCompoundName(knownCompound);

        assertThat(result).isPresent();
        assertEntityEquals(result.get(), expected);
    }

    // =========================================================================
    // getAllDepictedCompoundNames
    // =========================================================================

    @Test
    default void getAllDepictedCompoundNames_returnsAllDepictedCompounds() {
        List<CompoundName> result = repository().getAllDepictedCompoundNames();

        assertThat(result).contains(
                TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.name,
                TestChemistryIdentifiers.Compounds.FormicAcid.name);
        assertThat(result).doesNotContain(TestChemistryIdentifiers.Compounds.NotFound.name);
    }
}
