package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.biogeography.Bioregion;
import com.naturalist.biogeography.SacramentoValley;
import com.naturalist.biogeography.SouthernCascades;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantRepository.SpeciesRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PlantSpecies-specific identity constants and entity construction.
 */
interface PlantSpeciesRepositoryTest
        extends EntityRepositoryTest<PlantSpeciesName, PlantSpecies> {

    @Override
    PlantRepository.SpeciesRepository repository();

    @Override
    default TestEntitySource<PlantSpeciesName, PlantSpecies> source() {
        return db.getNamed(PlantSpeciesTestEntitySource.class);
    }

    @Override
    default PlantSpeciesName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.name;
    }

    @Override
    default List<PlantSpeciesName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name,
                TestPlantsIdentifiers.Plants.Borage.name
        );
    }

    @Override
    default PlantSpecies newEntity() {
        return new PlantSpecies(
                PlantSpeciesName.of(RandomValue.string()),
                TestPlantsIdentifiers.PlantGenera.Thymus.name,
                TaxonomicSpecies.of("species" + RandomValue.string()),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                GrowthHabit.FORB_HERB,
                LifeCycle.ANNUAL,
                Set.of(),
                Set.of()
        );
    }

    @Override
    default PlantSpecies ghostEntity() {
        return new PlantSpecies(
                PlantSpeciesName.of(RandomValue.string()),
                PlantGenusName.of("test-ghost-genus-xx"),
                TaxonomicSpecies.of("species" + RandomValue.string()),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                GrowthHabit.FORB_HERB,
                LifeCycle.ANNUAL,
                Set.of(),
                Set.of()
        );
    }

    @Override
    default PlantSpecies modifiedEntity(PlantSpecies original) {
        Set<Bioregion> flippedBioregions = original.nativeBioregions().isEmpty()
                ? Set.of(new SacramentoValley())
                : Set.of(new SouthernCascades());
        return new PlantSpecies(
                original.name(),
                TestPlantsIdentifiers.PlantGenera.Salvia.name,
                TaxonomicSpecies.of("species" + RandomValue.string()),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                GrowthHabit.FORB_HERB,
                LifeCycle.PERENNIAL,
                flippedBioregions,
                Set.of(CommonName.of("alt-" + RandomValue.string()))
        );
    }

    @Test
    default void getByGenusName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByGenusName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("genusName");
    }

    @Test
    default void getByGenusName_returnsSpeciesWithMatchingGenusName() {
        // Trifolium carries two catalogued species — enough to distinguish a real
        // genus join from a single-entity lookup.
        var results = repository().getByGenusName(TestPlantsIdentifiers.PlantGenera.Trifolium.name);

        assertThat(results)
                .extracting(PlantSpecies::name)
                .extracting(PlantSpeciesName::value)
                .contains("trifolium-incarnatum", "trifolium-repens");
    }

    @Test
    default void getByGenusName_returnsEmptyForUnknownGenus() {
        var results = repository().getByGenusName(TestPlantsIdentifiers.PlantGenera.NotFound.name);

        assertThat(results).isEmpty();
    }
}
