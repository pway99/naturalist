package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.exception.UniqueConstraintException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Abstract behavioral contract for {@link EntityRepository}. Identity is the
 * entity's name (ADR-021) — either an {@code EntityName} (slug) or a {@code EntityId}
 * (UUID); both are {@link Named}.
 *
 * <p>Every method is covered by three cases per ADR-002:
 * <ol>
 *   <li>Argument validation — null or invalid input is rejected before any query executes</li>
 *   <li>No-match — the method returns empty when no entities satisfy the query</li>
 *   <li>Expected result — the method returns the correct entities</li>
 * </ol>
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 */
public interface EntityRepositoryTest<
        NAME,
        ENTITY extends Named<NAME>> {

    Observer observer = Observer.forClass(EntityRepositoryTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    EntityRepository<NAME, ENTITY> repository();

    TestEntitySource<NAME, ENTITY> source();

    // =========================================================================
    // Identity hooks — concrete tests supply these
    // =========================================================================

    /**
     * A fictitious name guaranteed absent from the catalog.
     */
    NAME notFoundName();

    /**
     * At least two known entity names present in the test data. Two are required
     * to distinguish set-based query behavior from single-entity lookup.
     */
    List<NAME> knownEntityNames();

    // =========================================================================
    // Entity comparison — override if the entity has custom equality semantics
    // =========================================================================

    /**
     * Asserts that {@code actual} is structurally equal to {@code expected}. Default
     * uses recursive comparison. No id field to ignore — a {@code NamedEntity}
     * carries none.
     */
    default void assertEntityEquals(ENTITY actual, ENTITY expected) {
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    default void assertEntityListEqualsInAnyOrder(List<ENTITY> actual, List<ENTITY> expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    default void assertEntityListEquals(List<ENTITY> actual, List<ENTITY> expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(expected);
    }

    // =========================================================================
    // Write-side hooks — entity construction for insert/update tests
    // =========================================================================

    /**
     * A new valid entity with a unique name not in the catalog. Used for the insert
     * expected-result test.
     */
    ENTITY newEntity();

    /**
     * An entity whose name does not exist in the catalog. Used for the update
     * not-found test.
     */
    ENTITY ghostEntity();

    /**
     * The original entity with every mutable field changed to a distinct value.
     * {@code name} is carried forward unchanged.
     */
    ENTITY modifiedEntity(ENTITY original);

    // =========================================================================
    // getByName
    // =========================================================================

    @Test
    default void getByName_nullArgument() {
        assertThatThrownBy(() -> repository().getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }

    @Test
    default void getByName_unknownName_returnsEmpty() {
        assertThat(repository().getByName(notFoundName()))
                .isEmpty();
    }

    @Test
    default void getByName_knownName_returnsEntity() {
        NAME knownName = knownEntityNames().getFirst();
        ENTITY expected = source().getByName(knownName).orElseThrow();

        Optional<ENTITY> result = repository().getByName(knownName);

        assertThat(result).isPresent();
        assertEntityEquals(result.get(), expected);
    }

    // =========================================================================
    // getByEntityNameSet
    // =========================================================================

    @Test
    default void getByEntityNameSet_nullArgument() {
        assertThatThrownBy(() -> repository().getByEntityNameSet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("nameSet");
    }

    @Test
    default void getByEntityNameSet_emptySet_returnsEmptyList() {
        assertThat(repository().getByEntityNameSet(Set.of())).isEmpty();
    }

    @Test
    default void getByEntityNameSet_noMatchingNames_returnsEmptyList() {
        assertThat(repository().getByEntityNameSet(Set.of(notFoundName()))).isEmpty();
    }

    @Test
    default void getByEntityNameSet_partialMatch_returnsOnlyMatchingEntities() {
        List<NAME> known = knownEntityNames();
        Set<NAME> names = new HashSet<>();
        names.add(known.get(0));
        names.add(known.get(1));
        names.add(notFoundName());

        Set<NAME> knownNames = Set.of(known.get(0), known.get(1));
        List<ENTITY> expected = source().getByEntityNameSet(knownNames);

        List<ENTITY> result = repository().getByEntityNameSet(names);

        assertThat(result).hasSize(2);
        assertEntityListEqualsInAnyOrder(result, expected);
    }

    @Test
    default void getByEntityNameSet_allKnownNames_returnsAllMatchingEntities() {
        List<NAME> known = knownEntityNames();
        Set<NAME> names = Set.of(known.get(0), known.get(1));
        List<ENTITY> expected = source().getByEntityNameSet(names);

        List<ENTITY> result = repository().getByEntityNameSet(names);

        assertThat(result).hasSize(2);
        assertEntityListEquals(result, expected);
    }

    // =========================================================================
    // getPage — page size defaults to 2 to exercise multi-page boundaries
    // against the typical NamedTestEntitySource fixture (≥4 entities).
    // Lookahead-saturation semantics are covered at the kernel level by
    // {@code TestEntitySourcePageTest}; the contract test focuses on
    // boundary correctness and round-trip completeness.
    // =========================================================================

    /**
     * Page size used by the paging contract tests. Override to pick a different
     * size — the default of 2 forces multi-page behaviour for typical fixtures
     * without requiring extra test data.
     */
    default int pageSize() {
        return 2;
    }

    @Test
    default void getPage_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().getPage(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("pageRequest");
    }

    @Test
    default void getPage_firstPage_returnsContentWithinPageSize() {
        Page<ENTITY> page = repository().getPage(PageRequest.first(pageSize()));

        assertThat(page).isNotNull();
        assertThat(page.pageNumber()).isZero();
        assertThat(page.pageSize()).isEqualTo(pageSize());
        assertThat(page.content()).hasSizeLessThanOrEqualTo(pageSize());
    }

    @Test
    default void getPage_pastEnd_returnsEmptyPage() {
        Page<ENTITY> page = repository().getPage(PageRequest.of(10_000, pageSize()));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    default void getPage_consistentOrderingAcrossCalls() {
        Page<ENTITY> first = repository().getPage(PageRequest.first(pageSize()));
        Page<ENTITY> second = repository().getPage(PageRequest.first(pageSize()));

        assertThat(first.content().stream().map(Named::name).toList())
                .isEqualTo(second.content().stream().map(Named::name).toList());
    }

    @Test
    default void getPage_streamingThroughAllPagesYieldsEverySourceEntity() {
        List<ENTITY> walked = new ArrayList<>();
        int pageNumber = 0;
        int safetyBound = 10_000;
        while (pageNumber < safetyBound) {
            Page<ENTITY> page = repository().getPage(PageRequest.of(pageNumber, pageSize()));
            walked.addAll(page.content());
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }

        List<ENTITY> all = source().entityStream().toList();
        assertEntityListEqualsInAnyOrder(walked, all);
    }

    @Test
    default void getPage_lookaheadPopulatesPagesAheadKnownWhenMoreExist() {
        Page<ENTITY> page = repository().getPage(
                PageRequest.of(0, pageSize(), PageRequest.DEFAULT_LOOKAHEAD));

        long total = source().entityStream().count();
        if (total > pageSize()) {
            // At least one more page exists; the contract is that lookahead
            // surfaces it as either pagesAheadKnown > 0 or moreBeyondLookahead.
            assertThat(page.hasNext()).isTrue();
        }
        // pagesAheadKnown is bounded by lookahead regardless of total size.
        assertThat(page.pagesAheadKnown()).isLessThanOrEqualTo(PageRequest.DEFAULT_LOOKAHEAD);
    }

    // =========================================================================
    // insert
    // =========================================================================

    @Test
    default void insert_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().insert(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    /**
     * Inserting an entity whose name already exists must throw
     * {@link UniqueConstraintException}.
     */
    @Test
    default void insert_duplicateName_throwsUniqueConstraintException() {
        ENTITY existing = source().getByName(knownEntityNames().getFirst()).orElseThrow();

        assertThatThrownBy(() -> repository().insert(existing))
                .isInstanceOf(com.naturalist.exception.PrimaryKeyConstraintException.class);
    }

    /**
     * A successfully inserted entity must be immediately retrievable by name. The
     * persisted entity's full constraint graph is observed to catch
     * adapter-introduced invariant violations.
     */
    @Test
    default void insert_newEntity_isRetrievableByName() {
        ENTITY entity = newEntity();

        repository().insert(entity);

        Optional<ENTITY> byName = repository().getByName(entity.name());
        assertThat(byName).isPresent();
        ENTITY persisted = byName.get();
        assertEntityEquals(persisted, entity);

        var mo = observer.forMethod("insert_newEntity_isRetrievableByName");
        assertThat(mo.namedEntity(persisted, "persisted").violations()).isEmpty();
    }

    // =========================================================================
    // update
    // =========================================================================

    @Test
    default void update_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().update(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    default void update_unknownName_throwsEntityNotFoundException() {
        assertThatThrownBy(() -> repository().update(ghostEntity()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    default void update_existingEntity_isRetrievableWithNewValues() {
        ENTITY original = source().getByName(knownEntityNames().getFirst()).orElseThrow();
        ENTITY modified = modifiedEntity(original);

        repository().update(modified);

        ENTITY persisted = repository().getByName(original.name()).orElseThrow();
        assertThat(persisted.name()).isEqualTo(original.name());
        assertEntityEquals(persisted, modified);

        var mo = observer.forMethod("update_existingEntity_isRetrievableWithNewValues");
        assertThat(mo.namedEntity(persisted, "persisted").violations()).isEmpty();
    }
}
