package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryContractTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link PlantRepository.PlantEntityRepository}.
 * <p>
 * Inherits all 22 standard {@link EntityRepositoryContractTest} cases (ADR-002).
 * Supplies Plant-specific identity constants and entity construction.
 */
interface PlantEntityRepositoryTest
        extends EntityRepositoryContractTest<PlantId, PlantName, Plant> {

    @Override
    PlantRepository.PlantEntityRepository repository();

    @Override
    default TestEntitySource<PlantId, PlantName, Plant> source() {
        return db.get(PlantTestEntitySource.class);
    }

    @Override
    default PlantName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.name;
    }

    @Override
    default List<PlantName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine,
                TestPlantsIdentifiers.Plants.Borage
        );
    }

    @Override
    default PlantId notFoundId() {
        return PlantId.of(Long.MAX_VALUE);
    }

    @Override
    default Plant newEntity() {
        return new Plant(
                null,
                PlantName.of(RandomValue.string()),
                new TaxonomicClassification(
                        TaxonomicOrder.of(RandomValue.string()),
                        TaxonomicFamily.of(RandomValue.string()),
                        TaxonomicGenus.of(RandomValue.string()),
                        TaxonomicSpecies.of(RandomValue.string())
                ),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                Set.of(PlantRole.FOOD_CROP),
                PlantLifeForm.ANNUAL,
                false, null, null
        );
    }

    @Override
    default Plant ghostEntity() {
        return new Plant(
                PlantId.of(Long.MAX_VALUE),
                PlantName.of(RandomValue.string()),
                new TaxonomicClassification(
                        TaxonomicOrder.of(RandomValue.string()),
                        TaxonomicFamily.of(RandomValue.string()),
                        null, null
                ),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                Set.of(PlantRole.FOOD_CROP),
                PlantLifeForm.ANNUAL,
                false, null, null
        );
    }

    @Override
    default Plant modifiedEntity(Plant original) {
        return new Plant(
                original.id(),
                original.name(),
                new TaxonomicClassification(
                        TaxonomicOrder.of(RandomValue.string()),
                        TaxonomicFamily.of(RandomValue.string()),
                        TaxonomicGenus.of(RandomValue.string()),
                        TaxonomicSpecies.of(RandomValue.string())
                ),
                new Description(RandomValue.string(), RandomValue.string(),
                        RandomValue.string(), RandomValue.string()),
                Set.of(PlantRole.NITROGEN_FIXER),
                PlantLifeForm.PERENNIAL,
                !original.nativeToSacramentoValley(),
                RandomValue.string(),
                RandomValue.string()
        );
    }
}
