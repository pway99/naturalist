package com.naturalist.chemistry.compound;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link CompoundRepository.DepictionRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies CompoundDepiction-specific identity constants and entity construction.
 */
interface DepictionEntityRepositoryTest
        extends EntityRepositoryTest<DepictionId, CompoundDepiction> {

    @Override
    CompoundRepository.DepictionRepository repository();

    @Override
    default TestEntitySource<DepictionId, CompoundDepiction> source() {
        return db.getNamed(CompoundDepictionTestEntitySource.class);
    }

    @Override
    default DepictionId notFoundName() {
        return TestChemistryIdentifiers.Compounds.NotFound.depictionId;
    }

    @Override
    default List<DepictionId> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.depictionId,
                TestChemistryIdentifiers.Compounds.FormicAcid.depictionId);
    }

    @Override
    default CompoundDepiction newEntity() {
        // urea is the one catalog compound without an existing depiction —
        // satisfies both the FK constraint (urea exists in compounds-base.json)
        // and the unique-on-compoundName constraint (no depiction yet references it).
        return new CompoundDepiction(
                DepictionId.create(),
                CompoundName.of("urea"),
                RandomValue.string(),
                RandomValue.string());
    }

    @Override
    default CompoundDepiction ghostEntity() {
        // FK check is skipped on the update-not-found path because
        // EntityNotFoundException fires before preSaveChecks.
        return new CompoundDepiction(
                DepictionId.create(),
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                RandomValue.string());
    }

    @Override
    default CompoundDepiction modifiedEntity(CompoundDepiction original) {
        return new CompoundDepiction(
                original.id(),
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
        DepictionId expectedId = TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.depictionId;
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
