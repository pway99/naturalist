package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.List;
import java.util.Set;

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
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default PlantFamily ghostEntity() {
        return new PlantFamily(
                PlantFamilyName.of("test-ghost-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default PlantFamily modifiedEntity(PlantFamily original) {
        return new PlantFamily(
                original.name(),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
