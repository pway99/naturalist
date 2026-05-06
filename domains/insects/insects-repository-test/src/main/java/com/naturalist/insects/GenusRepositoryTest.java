package com.naturalist.insects;

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
 * Behavioral contract for {@link InsectRepository.GenusRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies InsectGenus-specific identity constants and entity construction.
 */
interface GenusRepositoryTest
        extends EntityRepositoryTest<InsectGenusName, InsectGenus> {

    @Override
    InsectRepository.GenusRepository repository();

    @Override
    default TestEntitySource<InsectGenusName, InsectGenus> source() {
        return db.getNamed(InsectGenusTestEntitySource.class);
    }

    @Override
    default InsectGenusName notFoundName() {
        return TestInsectsIdentifiers.InsectGenus.NotFound.name;
    }

    @Override
    default List<InsectGenusName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectGenus.Halictus.name,
                TestInsectsIdentifiers.InsectGenus.Andrena.name);
    }

    @Override
    default InsectGenus newEntity() {
        return new InsectGenus(
                InsectGenusName.of("test-genus-xx"),
                InsectFamilyName.of("tachinidae"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default InsectGenus ghostEntity() {
        // FK check is skipped because EntityNotFoundException fires first on
        // update; familyName slug here does not need to resolve.
        return new InsectGenus(
                InsectGenusName.of("test-ghost-xx"),
                InsectFamilyName.of("test-ghost-family-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of());
    }

    @Override
    default InsectGenus modifiedEntity(InsectGenus original) {
        return new InsectGenus(
                original.name(),
                InsectFamilyName.of("braconidae"),
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
