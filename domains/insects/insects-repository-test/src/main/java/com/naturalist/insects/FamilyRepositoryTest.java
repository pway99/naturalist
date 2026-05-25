package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantViolationException;
import com.naturalist.taxonomy.TaxonomicFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                InsectOrderName.of("diptera"),
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
                InsectOrderName.of("test-ghost-order-xx"),
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
                InsectOrderName.of("hymenoptera"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    default void getByOrderName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orderName");
    }

    @Test
    default void getByOrderName_returnsFamiliesWithMatchingOrderName() {
        InsectOrderName diptera = InsectOrderName.of("diptera");
        var results = repository().getByOrderName(diptera);
        assertThat(results)
                .extracting(InsectFamily::name)
                .extracting(InsectFamilyName::value)
                .contains("tachinidae");
    }

    @Test
    default void getByOrderName_returnsEmptyForUnknownOrder() {
        InsectOrderName unknown = InsectOrderName.of("zygentoma");
        var results = repository().getByOrderName(unknown);
        assertThat(results).isEmpty();
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
