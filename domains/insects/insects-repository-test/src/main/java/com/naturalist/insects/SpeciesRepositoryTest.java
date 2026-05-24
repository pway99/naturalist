package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link InsectRepository.SpeciesRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies InsectSpecies-specific identity constants and entity construction.
 */
interface SpeciesRepositoryTest
        extends EntityRepositoryTest<InsectSpeciesName, InsectSpecies> {

    @Override
    InsectRepository.SpeciesRepository repository();

    @Override
    default TestEntitySource<InsectSpeciesName, InsectSpecies> source() {
        return db.getNamed(InsectSpeciesTestEntitySource.class);
    }

    @Override
    default InsectSpeciesName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.name;
    }

    @Override
    default List<InsectSpeciesName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.TachinidFly.name,
                TestInsectsIdentifiers.InsectSpecies.BraconidWasp.name);
    }

    @Override
    default InsectSpecies newEntity() {
        return new InsectSpecies(
                InsectSpeciesName.of("test-species-xx"),
                taxonomy(),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Override
    default InsectSpecies ghostEntity() {
        return new InsectSpecies(
                InsectSpeciesName.of("test-ghost-xx"),
                taxonomy(),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Override
    default InsectSpecies modifiedEntity(InsectSpecies original) {
        return new InsectSpecies(
                original.name(),
                taxonomy(),
                description(),
                Set.of(),
                RandomValue.string(),
                new InsectSpecies.IdentificationFeatures(List.of(RandomValue.string())),
                null,
                null, null, null, null,
                null,
                new InsectSpecies.Voltinism(
                        InsectSpecies.Voltinism.VoltinismPattern.UNIVOLTINE,
                        RandomValue.string()),
                null,
                null,
                new InsectSpecies.GardenConnections(
                        List.of(RandomValue.string()),
                        RandomValue.string(),
                        RandomValue.string()),
                new InsectSpecies.BeneficialProfile(
                        RandomValue.string(),
                        RandomValue.string(),
                        RandomValue.string()),
                null);
    }

    private static TaxonomicClassification taxonomy() {
        return new TaxonomicClassification(
                TaxonomicOrder.of("Coleoptera"),
                TaxonomicFamily.of("Carabidae"),
                TaxonomicGenus.of("Carabus"),
                TaxonomicSpecies.of("nemoralis"));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    default void getByGenusEpithet_rejectsNull() {
        assertThatThrownBy(() -> repository().getByGenusEpithet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("genusEpithet");
    }

    @Test
    default void getByGenusEpithet_returnsSpeciesWithMatchingGenus() {
        TaxonomicGenus battus = TaxonomicGenus.of("Battus");

        var results = repository().getByGenusEpithet(battus);

        assertThat(results)
                .extracting(InsectSpecies::name)
                .extracting(InsectSpeciesName::value)
                .contains("battus-philenor");
    }

    @Test
    default void getByGenusEpithet_returnsEmptyForUnknownGenus() {
        TaxonomicGenus unknown = TaxonomicGenus.of("Unobtainium");

        var results = repository().getByGenusEpithet(unknown);

        assertThat(results).isEmpty();
    }
}
