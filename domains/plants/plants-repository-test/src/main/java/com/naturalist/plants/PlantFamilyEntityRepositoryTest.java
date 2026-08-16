package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantRepository.PlantFamilyEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PlantFamily-specific identity constants and entity construction.
 */
interface PlantFamilyEntityRepositoryTest
        extends EntityRepositoryTest<PlantFamilyName, PlantFamily> {

    @Override
    PlantRepository.PlantFamilyEntityRepository repository();

    @Override
    default TestEntitySource<PlantFamilyName, PlantFamily> source() {
        return db.getNamed(PlantFamilyTestEntitySource.class);
    }

    @Override
    default PlantFamilyName notFoundName() {
        return TestPlantsIdentifiers.PlantFamilies.NotFound.name;
    }

    @Override
    default List<PlantFamilyName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.PlantFamilies.Aristolochiaceae.name,
                TestPlantsIdentifiers.PlantFamilies.Lamiaceae.name);
    }

    @Override
    default PlantFamily newEntity() {
        return new PlantFamily(
                PlantFamilyName.of("test-family-xx"),
                TestPlantsIdentifiers.PlantOrders.Lamiales.name,
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default PlantFamily ghostEntity() {
        return new PlantFamily(
                PlantFamilyName.of("test-ghost-xx"),
                TestPlantsIdentifiers.PlantOrders.Lamiales.name,
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default PlantFamily modifiedEntity(PlantFamily original) {
        return new PlantFamily(
                original.name(),
                TestPlantsIdentifiers.PlantOrders.Piperales.name,
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    default void getByOrderName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orderName");
    }

    @Test
    default void getByOrderName_returnsFamiliesWithMatchingOrderName() {
        // Malpighiales carries two catalogued families — enough to distinguish a real
        // order join from a single-entity lookup.
        var results = repository().getByOrderName(
                TestPlantsIdentifiers.PlantOrders.Malpighiales.name);

        assertThat(results)
                .extracting(PlantFamily::name)
                .extracting(PlantFamilyName::value)
                .contains("passifloraceae", "violaceae");
    }

    @Test
    default void getByOrderName_returnsEmptyForUnknownOrder() {
        var results = repository().getByOrderName(
                TestPlantsIdentifiers.PlantOrders.NotFound.name);

        assertThat(results).isEmpty();
    }
}
