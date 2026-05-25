package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicGenus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                InsectOrderName.of("diptera"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    default InsectGenus ghostEntity() {
        // FK check is skipped because EntityNotFoundException fires first on
        // update; familyName slug here does not need to resolve.
        return new InsectGenus(
                InsectGenusName.of("test-ghost-xx"),
                InsectFamilyName.of("test-ghost-family-xx"),
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    default InsectGenus modifiedEntity(InsectGenus original) {
        return new InsectGenus(
                original.name(),
                InsectFamilyName.of("braconidae"),
                InsectOrderName.of("hymenoptera"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
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

    @Test
    default void getByFamilyName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByFamilyName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("familyName");
    }

    @Test
    default void getByFamilyName_returnsGeneraWithMatchingFamilyName() {
        InsectFamilyName halictidae = InsectFamilyName.of("halictidae");

        var results = repository().getByFamilyName(halictidae);

        assertThat(results)
                .extracting(InsectGenus::name)
                .extracting(InsectGenusName::value)
                .contains("halictus");
    }

    @Test
    default void getByFamilyName_returnsEmptyForUnknownFamily() {
        InsectFamilyName unknown = InsectFamilyName.of("unobtainium-idae");

        var results = repository().getByFamilyName(unknown);

        assertThat(results).isEmpty();
    }

    @Test
    default void getByOrderName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orderName");
    }

    @Test
    default void getByOrderName_returnsGeneraWithMatchingOrderName() {
        InsectOrderName hymenoptera = InsectOrderName.of("hymenoptera");
        var results = repository().getByOrderName(hymenoptera);
        assertThat(results)
                .extracting(InsectGenus::name)
                .extracting(InsectGenusName::value)
                .contains("halictus");
    }

    @Test
    default void getByOrderName_returnsEmptyForUnknownOrder() {
        InsectOrderName unknown = InsectOrderName.of("zygentoma");
        var results = repository().getByOrderName(unknown);
        assertThat(results).isEmpty();
    }
}
