package com.naturalist.insects;

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
 * Behavioral contract for {@link InsectRepository.FamilyRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies InsectFamily-specific identity constants and entity construction.
 */
interface FamilyRepositoryTest
        extends EntityRepositoryTest<InsectFamilyName, InsectFamily> {

    @Override
    InsectRepository.FamilyRepository repository();

    @Override
    default TestEntitySource<InsectFamilyName, InsectFamily> source() {
        return db.getNamed(InsectFamilyTestEntitySource.class);
    }

    @Override
    default InsectFamilyName notFoundName() {
        return TestInsectsIdentifiers.InsectFamily.NotFound.name;
    }

    @Override
    default List<InsectFamilyName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFamily.Tachinidae.name,
                TestInsectsIdentifiers.InsectFamily.Braconidae.name);
    }

    @Override
    default InsectFamily newEntity() {
        return new InsectFamily(
                InsectFamilyName.of("test-family-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    default InsectFamily ghostEntity() {
        return new InsectFamily(
                InsectFamilyName.of("test-ghost-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    default InsectFamily modifiedEntity(InsectFamily original) {
        return new InsectFamily(
                original.name(),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null,
                null,
                null,
                null,
                null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
