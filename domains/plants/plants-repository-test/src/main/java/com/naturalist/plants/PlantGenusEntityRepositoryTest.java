package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link PlantRepository.PlantGenusEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PlantGenus-specific identity constants and entity construction.
 */
interface PlantGenusEntityRepositoryTest
        extends EntityRepositoryTest<PlantGenusName, PlantGenus> {

    @Override
    PlantRepository.PlantGenusEntityRepository repository();

    @Override
    default TestEntitySource<PlantGenusName, PlantGenus> source() {
        return db.getNamed(PlantGenusTestEntitySource.class);
    }

    @Override
    default PlantGenusName notFoundName() {
        return TestPlantsIdentifiers.PlantGenera.NotFound.name;
    }

    @Override
    default List<PlantGenusName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.PlantGenera.Thymus.name,
                TestPlantsIdentifiers.PlantGenera.Salvia.name);
    }

    @Override
    default PlantGenus newEntity() {
        return new PlantGenus(
                PlantGenusName.of("test-genus-xx"),
                PlantFamilyName.of("apiaceae"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default PlantGenus ghostEntity() {
        // FK check is skipped because EntityNotFoundException fires first on
        // update; familyName slug here does not need to resolve.
        return new PlantGenus(
                PlantGenusName.of("test-ghost-xx"),
                PlantFamilyName.of("test-ghost-family-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default PlantGenus modifiedEntity(PlantGenus original) {
        return new PlantGenus(
                original.name(),
                PlantFamilyName.of("boraginaceae"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
