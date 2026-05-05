package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.biogeography.Bioregion;
import com.naturalist.biogeography.SacramentoValley;
import com.naturalist.biogeography.SouthernCascades;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.*;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link PlantRepository.PlantEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies Plant-specific identity constants and entity construction.
 */
interface PlantEntityRepositoryTest
        extends EntityRepositoryTest<PlantName, Plant> {

    @Override
    PlantRepository.PlantEntityRepository repository();

    @Override
    default TestEntitySource<PlantName, Plant> source() {
        return db.getNamed(PlantTestEntitySource.class);
    }

    @Override
    default PlantName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.name;
    }

    @Override
    default List<PlantName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name,
                TestPlantsIdentifiers.Plants.Borage.name
        );
    }

    @Override
    default Plant newEntity() {
        return new Plant(
                PlantName.of(RandomValue.string()),
                new TaxonomicClassification(
                        TaxonomicOrder.of("Order" + RandomValue.string()),
                        TaxonomicFamily.of("Family" + RandomValue.string()),
                        TaxonomicGenus.of("Genus" + RandomValue.string()),
                        TaxonomicSpecies.of("species" + RandomValue.string())
                ),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                Set.of(PlantRole.FOOD_CROP),
                PlantLifeForm.ANNUAL,
                Set.of(),
                Set.of()
        );
    }

    @Override
    default Plant ghostEntity() {
        return new Plant(
                PlantName.of(RandomValue.string()),
                new TaxonomicClassification(
                        TaxonomicOrder.of("Order" + RandomValue.string()),
                        TaxonomicFamily.of("Family" + RandomValue.string()),
                        TaxonomicGenus.of("Genus" + RandomValue.string()),
                        TaxonomicSpecies.of("species" + RandomValue.string())
                ),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                Set.of(PlantRole.FOOD_CROP),
                PlantLifeForm.ANNUAL,
                Set.of(),
                Set.of()
        );
    }

    @Override
    default Plant modifiedEntity(Plant original) {
        Set<Bioregion> flippedBioregions = original.nativeBioregions().isEmpty()
                ? Set.of(new SacramentoValley())
                : Set.of(new SouthernCascades());
        return new Plant(
                original.name(),
                new TaxonomicClassification(
                        TaxonomicOrder.of("Order" + RandomValue.string()),
                        TaxonomicFamily.of("Family" + RandomValue.string()),
                        TaxonomicGenus.of("Genus" + RandomValue.string()),
                        TaxonomicSpecies.of("species" + RandomValue.string())
                ),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                Set.of(PlantRole.NITROGEN_FIXER),
                PlantLifeForm.PERENNIAL,
                flippedBioregions,
                Set.of(CommonName.of("alt-" + RandomValue.string()))
        );
    }
}
