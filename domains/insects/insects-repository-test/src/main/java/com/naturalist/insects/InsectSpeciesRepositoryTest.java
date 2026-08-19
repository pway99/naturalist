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
interface InsectSpeciesRepositoryTest
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
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                TestInsectsIdentifiers.InsectSpecies.ColiasEurytheme.name);
    }

    @Override
    default InsectSpecies newEntity() {
        return new InsectSpecies(
                InsectSpeciesName.of("test-species-xx"),
                InsectGenusName.of("carabus"),
                TaxonomicSpecies.of("nemoralis"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null,
                null, null, null);
    }

    @Override
    default InsectSpecies ghostEntity() {
        return new InsectSpecies(
                InsectSpeciesName.of("test-ghost-xx"),
                InsectGenusName.of("carabus"),
                TaxonomicSpecies.of("ghost"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null,
                null, null, null);
    }

    @Override
    default InsectSpecies modifiedEntity(InsectSpecies original) {
        return new InsectSpecies(
                original.name(),
                InsectGenusName.of("carabus"),
                TaxonomicSpecies.of("nemoralis"),
                description(),
                Set.of(),
                RandomValue.string(),
                null,
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

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    default void getByGenusName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByGenusName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("genusName");
    }

    @Test
    default void getByGenusName_returnsSpeciesWithMatchingGenusName() {
        InsectGenusName halictus = TestInsectsIdentifiers.InsectGenus.Halictus.name;
        InsectSpecies seeded = newEntity();
        InsectSpecies underHalictus = new InsectSpecies(
                seeded.name(),
                halictus,
                seeded.epithet(),
                seeded.description(),
                seeded.commonNames(),
                null, null, null, null,
                null, null, null, null, null);
        repository().insert(underHalictus);

        var results = repository().getByGenusName(halictus);

        assertThat(results)
                .extracting(InsectSpecies::name)
                .extracting(InsectSpeciesName::value)
                .contains(seeded.name().value());
    }

    @Test
    default void getByGenusName_returnsEmptyForUnknownGenus() {
        InsectGenusName unknown = TestInsectsIdentifiers.InsectGenus.NotFound.name;

        var results = repository().getByGenusName(unknown);

        assertThat(results).isEmpty();
    }

}
