package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Named;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Abstract behavioral contract for an {@link EntityQuery} adapter — the query-side
 * analogue of {@link EntityRepositoryTest}.
 *
 * <p>Every method is covered by three cases per ADR-002:
 * <ol>
 *   <li>Argument validation — null or invalid input is rejected before any query runs</li>
 *   <li>No-match — the method returns an empty result when no entities satisfy the query</li>
 *   <li>Expected result — the method returns the correct entities</li>
 * </ol>
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <EC>   the behavioral collection returned by {@code findByNameSet}
 */
public interface EntityQueryContractTest<
        NAME,
        E extends Named<NAME>,
        EC extends BehavioralCollection<E>> {

    EntityQuery<NAME, E, EC> query();

    /**
     * A fictitious name guaranteed absent from the catalog.
     */
    NAME notFoundName();

    /**
     * At least two known entity names present in the test data. Two are required to
     * distinguish set-based query behavior from single-entity lookup.
     */
    List<NAME> knownEntityNames();

    @Test
    default void getByName_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> query().getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }

    @Test
    default void getByName_unknownName_returnsEmpty() {
        assertThat(query().getByName(notFoundName())).isEmpty();
    }

    @Test
    default void getByName_knownName_returnsEntity() {
        NAME known = knownEntityNames().getFirst();

        Optional<E> result = query().getByName(known);

        assertThat(result).isPresent();
        assertThat(result.get().key()).isEqualTo(known);
    }

    @Test
    default void findByNameSet_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> query().findByNameSet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("names");
    }

    @Test
    default void findByNameSet_emptySet_returnsEmptyCollection() {
        EC collection = query().findByNameSet(Set.of());

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    default void findByNameSet_noMatchingNames_returnsEmptyCollection() {
        EC collection = query().findByNameSet(Set.of(notFoundName()));

        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    default void findByNameSet_partialMatch_returnsOnlyMatchingEntities() {
        List<NAME> known = knownEntityNames();
        Set<NAME> names = new HashSet<>();
        names.add(known.get(0));
        names.add(known.get(1));
        names.add(notFoundName());

        EC collection = query().findByNameSet(names);

        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(e -> e.key()))
                .containsExactlyInAnyOrder(known.get(0), known.get(1));
    }

    @Test
    default void findByNameSet_allKnownNames_returnsAllMatchingEntities() {
        List<NAME> known = knownEntityNames();
        Set<NAME> names = Set.of(known.get(0), known.get(1));

        EC collection = query().findByNameSet(names);

        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(e -> e.key()))
                .containsExactlyInAnyOrder(known.get(0), known.get(1));
    }

    // ---------------------------------------------------------------------------------
    // findPage — query-side delegation to the repository's paged read.
    // Boundary and round-trip correctness are covered by
    // {@code EntityRepositoryTest#getPage_*}; this contract verifies argument
    // validation and that the query forwards rather than swallowing.
    // ---------------------------------------------------------------------------------

    @Test
    default void findPage_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> query().findPage(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("pageRequest");
    }

    @Test
    default void findPage_firstPage_returnsNonNullPage() {
        Page<E> page = query().findPage(PageRequest.first(2));

        assertThat(page).isNotNull();
        assertThat(page.content()).isNotNull();
        assertThat(page.pageNumber()).isZero();
        assertThat(page.pageSize()).isEqualTo(2);
    }
}
