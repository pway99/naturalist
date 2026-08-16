package com.naturalist.plants.heritage;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.plants.TestPlantsIdentifiers;
import com.naturalist.plants.cultivar.CultivarName;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link SeedLineageRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies SeedLineage-specific identity constants and entity construction.
 * <p>
 * The cultivar-must-be-OPEN_POLLINATED rule lives at the service layer, not in
 * the record invariants — fixtures here reference open-pollinated cultivars
 * (italian-pear-nicks, amish-paste) so the constructed lineages are
 * service-layer-valid as well as record-valid.
 */
interface SeedLineageRepositoryTest
        extends EntityRepositoryTest<SeedLineageName, SeedLineage> {

    @Override
    SeedLineageRepository repository();

    @Override
    default TestEntitySource<SeedLineageName, SeedLineage> source() {
        return db.getNamed(SeedLineageTestEntitySource.class);
    }

    @Override
    default SeedLineageName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.seedLineageName;
    }

    @Override
    default List<SeedLineageName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.AmishPaste.Lineages.BakerCreek,
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.ItalianPearNicks.Lineages.Original
        );
    }

    @Override
    default SeedLineage newEntity() {
        return new SeedLineage(
                SeedLineageName.of("test-" + RandomValue.string()),
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.ItalianPearNicks.name,
                provenance(),
                description(),
                2026,
                "earliest ripening, best flavour under heat",
                null);
    }

    @Override
    default SeedLineage ghostEntity() {
        return new SeedLineage(
                SeedLineageName.of("ghost-" + RandomValue.string()),
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.AmishPaste.name,
                provenance(),
                description(),
                0,
                null,
                "fictitious test fixture");
    }

    @Override
    default SeedLineage modifiedEntity(SeedLineage original) {
        CultivarName flippedCultivar = original.cultivarName()
                .equals(TestPlantsIdentifiers.Plants.Tomato.Cultivars.ItalianPearNicks.name)
                ? TestPlantsIdentifiers.Plants.Tomato.Cultivars.AmishPaste.name
                : TestPlantsIdentifiers.Plants.Tomato.Cultivars.ItalianPearNicks.name;
        int flippedYear = original.adaptationStartYear() == 0 ? 2026 : 0;
        return new SeedLineage(
                original.name(),
                flippedCultivar,
                provenance(),
                description(),
                flippedYear,
                RandomValue.string(),
                RandomValue.string());
    }

    private static Provenance provenance() {
        return new Provenance(
                "Originator " + RandomValue.string(),
                "Origin Location " + RandomValue.string(),
                RandomValue.integer(),
                RandomValue.string());
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    default void getByCultivarName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByCultivarName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("cultivarName");
    }

    @Test
    default void getByCultivarName_returnsLineagesWithMatchingCultivarName() {
        var results = repository().getByCultivarName(
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.AmishPaste.name);

        assertThat(results)
                .extracting(SeedLineage::name)
                .extracting(SeedLineageName::value)
                .contains("amish-paste-baker-creek");
    }

    @Test
    default void getByCultivarName_returnsEmptyForUnknownCultivar() {
        var results = repository().getByCultivarName(
                TestPlantsIdentifiers.Plants.NotFound.cultivarName);

        assertThat(results).isEmpty();
    }
}
