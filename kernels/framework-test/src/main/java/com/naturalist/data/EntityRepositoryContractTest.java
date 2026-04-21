package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.exception.UniqueConstraintException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Abstract behavioral contract for {@link EntityRepository}.
 * <p>
 * Every method is covered by three cases per ADR-002:
 * <ol>
 *   <li>Argument validation — null or invalid input is rejected before any query executes</li>
 *   <li>No-match — the method returns empty when no entities satisfy the query</li>
 *   <li>Expected result — the method returns the correct entities</li>
 * </ol>
 * <p>
 * Concrete test interfaces extend this and provide identity constants plus entity
 * construction methods for insert/update tests.
 *
 * @param <ID>     the entity's persistence id type
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the entity type
 */
public interface EntityRepositoryContractTest<
        ID extends PersistenceId<?>,
        NAME extends EntityName<?>,
        ENTITY extends Entity<ID, NAME>> {

    Observer observer = Observer.forClass(EntityRepositoryContractTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    EntityRepository<ID, NAME, ENTITY> repository();

    TestEntitySource<ID, NAME, ENTITY> source();

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

    /**
     * An id guaranteed absent from the data source — typically {@code XxxId.of(Long.MAX_VALUE)}.
     */
    ID notFoundId();

    // =========================================================================
    // Entity comparison — override if the entity has custom equality semantics
    // =========================================================================

    /**
     * Asserts that {@code actual} is structurally equal to {@code expected}, ignoring
     * the persistence id. Default uses recursive comparison ignoring {@code "id"}.
     */
    default void assertEntityEquals(ENTITY actual, ENTITY expected) {
        assertThat(actual)
                .usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(expected);
    }

    /**
     * Asserts that {@code actual} contains the same elements as {@code expected} in
     * any order, ignoring persistence ids. Default uses recursive field-by-field
     * element comparison ignoring {@code "id"}.
     */
    default void assertEntityListEqualsInAnyOrder(List<ENTITY> actual, List<ENTITY> expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * Asserts that {@code actual} contains the same elements as {@code expected} in
     * the same order, ignoring persistence ids.
     */
    default void assertEntityListEquals(List<ENTITY> actual, List<ENTITY> expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyElementsOf(expected);
    }

    // =========================================================================
    // Write-side hooks — entity construction for insert/update tests
    // =========================================================================

    /**
     * A new valid entity with null id and a unique name not in the catalog.
     * Used for the insert expected-result test.
     */
    ENTITY newEntity();

    /**
     * An entity with a non-existent id. Used for the update not-found test.
     */
    ENTITY ghostEntity();

    /**
     * The original entity with every mutable field changed to a distinct value.
     * {@code id} and {@code name} are carried forward unchanged.
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
        // Arrange
        NAME knownName = knownEntityNames().getFirst();
        ENTITY expected = source().getByName(knownName).orElseThrow();

        // Act
        Optional<ENTITY> result = repository().getByName(knownName);

        // Assert
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

    /**
     * The set contains two known names and one fictitious name. Only the known entities
     * must be returned — the repository must not expand a partial-match set into a
     * full table scan or silently ignore the unknown name. Two known values in the
     * result distinguish set-based query behavior from single-entity lookup.
     */
    @Test
    default void getByEntityNameSet_partialMatch_returnsOnlyMatchingEntities() {
        // Arrange — two known + one fictitious
        List<NAME> known = knownEntityNames();
        Set<NAME> names = new HashSet<>();
        names.add(known.get(0));
        names.add(known.get(1));
        names.add(notFoundName());

        Set<NAME> knownNames = Set.of(known.get(0), known.get(1));
        List<ENTITY> expected = source().getByEntityNameSet(knownNames);

        // Act
        List<ENTITY> result = repository().getByEntityNameSet(names);

        // Assert
        assertThat(result).hasSize(2);
        assertEntityListEqualsInAnyOrder(result, expected);
    }

    @Test
    default void getByEntityNameSet_allKnownNames_returnsAllMatchingEntities() {
        // Arrange
        List<NAME> known = knownEntityNames();
        Set<NAME> names = Set.of(known.get(0), known.get(1));
        List<ENTITY> expected = source().getByEntityNameSet(names);

        // Act
        List<ENTITY> result = repository().getByEntityNameSet(names);

        // Assert
        assertThat(result).hasSize(2);
        assertEntityListEquals(result, expected);
    }

    // =========================================================================
    // getById
    // =========================================================================

    @Test
    default void getById_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().getById(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    default void getById_unknownId_returnsEmpty() {
        assertThat(repository().getById(notFoundId())).isEmpty();
    }

    @Test
    default void getById_knownId_returnsEntity() {
        // Arrange
        ENTITY expected = source().getByName(knownEntityNames().getFirst()).orElseThrow();

        // Act & Assert
        Optional<ENTITY> result = repository().getById(expected.id());
        assertThat(result).isPresent();
        assertEntityEquals(result.get(), expected);
    }

    // =========================================================================
    // getByIdSet
    // =========================================================================

    @Test
    default void getByIdSet_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().getByIdSet(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    default void getByIdSet_emptySet_returnsEmptyList() {
        assertThat(repository().getByIdSet(Set.of())).isEmpty();
    }

    @Test
    default void getByIdSet_noMatchingIds_returnsEmptyList() {
        assertThat(repository().getByIdSet(Set.of(notFoundId()))).isEmpty();
    }

    /**
     * The set contains two known ids and one that will never exist. Only the known entities
     * must be returned. Two known values in the result distinguish set-based query behavior
     * from single-entity lookup.
     */
    @Test
    default void getByIdSet_partialMatch_returnsOnlyMatchingEntities() {
        // Arrange — two known + one fictitious
        List<NAME> known = knownEntityNames();
        ENTITY first = source().getByName(known.get(0)).orElseThrow();
        ENTITY second = source().getByName(known.get(1)).orElseThrow();
        Set<ID> ids = new HashSet<>();
        ids.add(first.id());
        ids.add(second.id());
        ids.add(notFoundId());

        // Act
        List<ENTITY> result = repository().getByIdSet(ids);

        // Assert
        assertThat(result).hasSize(2);
        assertEntityListEqualsInAnyOrder(result, List.of(first, second));
    }

    @Test
    default void getByIdSet_allKnownIds_returnsAllMatchingEntities() {
        // Arrange
        List<NAME> known = knownEntityNames();
        ENTITY first = source().getByName(known.get(0)).orElseThrow();
        ENTITY second = source().getByName(known.get(1)).orElseThrow();
        Set<ID> ids = Set.of(first.id(), second.id());

        // Act
        List<ENTITY> result = repository().getByIdSet(ids);

        // Assert
        assertThat(result).hasSize(2);
        assertEntityListEqualsInAnyOrder(result, List.of(first, second));
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
     * Inserting a null-id entity whose name already exists must throw
     * {@link UniqueConstraintException} before any id is assigned.
     */
    @SuppressWarnings("unchecked")
    @Test
    default void insert_duplicateName_throwsUniqueConstraintException() {
        // Arrange — null id so the PK check is not reached; name collision fires first
        ENTITY existing = source().getByName(knownEntityNames().getFirst()).orElseThrow();
        ENTITY duplicate = (ENTITY) existing.withId(null);

        // Act & Assert
        assertThatThrownBy(() -> repository().insert(duplicate))
                .isInstanceOf(UniqueConstraintException.class);
    }

    /**
     * A successfully inserted entity must be immediately retrievable by both name and
     * the id assigned at insert time. The assigned id must be non-null. The persisted
     * entity's full constraint graph is observed to catch adapter-introduced invariant
     * violations.
     */
    @Test
    default void insert_newEntity_isRetrievableByNameAndById() {
        // Arrange
        ENTITY entity = newEntity();

        // Act
        repository().insert(entity);

        // Assert
        Optional<ENTITY> byName = repository().getByName(entity.name());
        assertThat(byName).isPresent();
        ENTITY persisted = byName.get();
        assertThat(persisted.id()).isNotNull();
        assertEntityEquals(persisted, entity);

        // Observe — walks the full constraint graph to catch adapter serialization drift
        var mo = observer.forMethod("insert_newEntity_isRetrievableByNameAndById");
        assertThat(mo.entity(persisted, "persisted").violations()).isEmpty();
    }

    // =========================================================================
    // update
    // =========================================================================

    @Test
    default void update_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().update(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    /**
     * Issuing an update for an id that does not exist must throw {@link EntityNotFoundException}.
     */
    @Test
    default void update_unknownId_throwsEntityNotFoundException() {
        assertThatThrownBy(() -> repository().update(ghostEntity()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /**
     * Every mutable field must be modified and the updated values must be persisted
     * accurately. {@code id} and {@code name} are immutable — they are carried forward
     * from the original and verified unchanged. The persisted entity's full constraint
     * graph is observed to catch adapter-introduced invariant violations.
     */
    @Test
    default void update_existingEntity_isRetrievableWithNewValues() {
        // Arrange
        ENTITY original = source().getByName(knownEntityNames().getFirst()).orElseThrow();
        ENTITY modified = modifiedEntity(original);

        // Act
        repository().update(modified);

        // Assert
        ENTITY persisted = repository().getByName(original.name()).orElseThrow();
        assertThat(persisted.id()).isEqualTo(original.id());
        assertThat(persisted.name()).isEqualTo(original.name());
        assertEntityEquals(persisted, modified);

        // Observe — walks the full constraint graph to catch adapter serialization drift
        var mo = observer.forMethod("update_existingEntity_isRetrievableWithNewValues");
        assertThat(mo.entity(persisted, "persisted").violations()).isEmpty();
    }
}
