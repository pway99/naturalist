package com.naturalist.insects;

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
 * Behavioral contract for {@link InsectRepository.FamilyRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies InsectFamilyInfo-specific identity constants and entity construction.
 */
interface InsectFamilyRepositoryTest
        extends EntityRepositoryTest<InsectFamilyName, InsectFamilyInfo> {

    @Override
    InsectRepository.FamilyRepository repository();

    @Override
    default TestEntitySource<InsectFamilyName, InsectFamilyInfo> source() {
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
    default InsectFamilyInfo newEntity() {
        return new InsectFamilyInfo(
                InsectFamilyName.of("test-family-xx"),
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default InsectFamilyInfo ghostEntity() {
        return new InsectFamilyInfo(
                InsectFamilyName.of("test-ghost-xx"),
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default InsectFamilyInfo modifiedEntity(InsectFamilyInfo original) {
        return new InsectFamilyInfo(
                original.name(),
                InsectOrderName.of("hymenoptera"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
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
                .extracting(InsectFamilyInfo::name)
                .extracting(InsectFamilyName::value)
                .contains("tachinidae");
    }

    @Test
    default void getByOrderName_returnsEmptyForUnknownOrder() {
        InsectOrderName unknown = InsectOrderName.of("zygentoma");
        var results = repository().getByOrderName(unknown);
        assertThat(results).isEmpty();
    }

    @Test
    default void getByOrderNames_rejectsNull() {
        assertThatThrownBy(() -> repository().getByOrderNames(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orderNames");
    }

    @Test
    default void getByOrderNames_partialMatch_returnsOnlyKnownOrders() {
        var results = repository().getByOrderNames(Set.of(
                InsectOrderName.of("diptera"),
                InsectOrderName.of("zygentoma")));
        assertThat(results)
                .extracting(InsectFamilyInfo::name)
                .extracting(InsectFamilyName::value)
                .contains("tachinidae");
        assertThat(results).allSatisfy(f ->
                assertThat(f.orderName()).isEqualTo(InsectOrderName.of("diptera")));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
