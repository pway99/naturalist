package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Named;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Abstract behavioral contract for an {@link EntityCommand} adapter — the write-side
 * analogue of {@link EntityQueryContractTest}.
 *
 * <p>Each method is covered by three cases per ADR-002:
 * <ul>
 *   <li>{@code insert} — null arg → {@link InvariantViolationException};
 *       duplicate name → {@link PrimaryKeyConstraintException};
 *       new entity → retrievable via {@link #query()}.</li>
 *   <li>{@code update} — null arg → {@link InvariantViolationException};
 *       ghost entity → {@link EntityNotFoundException};
 *       modified entity → retrievable with new values.</li>
 * </ul>
 *
 * <p>Post-state verification reads through the public {@link EntityQuery} —
 * <b>not</b> the package-private repository — so the contract observes only what a
 * domain consumer would see at the api boundary.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <EC>   the behavioral collection returned by the verifying query
 */
public interface EntityCommandContractTest<
        NAME,
        E extends Named<NAME>,
        EC extends BehavioralCollection<E>> {

    Observer observer = Observer.forClass(EntityCommandContractTest.class);

    EntityCommand<NAME, E> command();

    EntityQuery<NAME, E, EC> query();

    TestEntitySource<NAME, E> source();

    /**
     * A fictitious name guaranteed absent from the catalog.
     */
    NAME notFoundName();

    /**
     * At least two known entity names present in the test data.
     */
    List<NAME> knownEntityNames();

    /**
     * A new valid entity with a unique name not in the catalog.
     */
    E newEntity();

    /**
     * An entity whose name does not exist in the catalog.
     */
    E ghostEntity();

    /**
     * The original entity with every mutable field changed to a distinct value.
     * {@code name} is carried forward unchanged.
     */
    E modifiedEntity(E original);

    default void assertEntityEquals(E actual, E expected) {
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    // =========================================================================
    // insert
    // =========================================================================

    @Test
    default void insert_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> command().insert(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    default void insert_duplicateName_throwsPrimaryKeyConstraintException() {
        E existing = source().getByName(knownEntityNames().getFirst()).orElseThrow();

        assertThatThrownBy(() -> command().insert(existing))
                .isInstanceOf(PrimaryKeyConstraintException.class);
    }

    @Test
    default void insert_newEntity_isRetrievableByName() {
        E entity = newEntity();

        command().insert(entity);

        Optional<E> byName = query().getByName(entity.key());
        assertThat(byName).isPresent();
        E persisted = byName.get();
        assertEntityEquals(persisted, entity);

        var mo = observer.forMethod("insert_newEntity_isRetrievableByName");
        assertThat(mo.namedEntity(persisted, "persisted").violations()).isEmpty();
    }

    // =========================================================================
    // update
    // =========================================================================

    @Test
    default void update_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> command().update(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    default void update_unknownName_throwsEntityNotFoundException() {
        assertThatThrownBy(() -> command().update(ghostEntity()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    default void update_existingEntity_isRetrievableWithNewValues() {
        E original = source().getByName(knownEntityNames().getFirst()).orElseThrow();
        E modified = modifiedEntity(original);

        command().update(modified);

        E persisted = query().getByName(original.key()).orElseThrow();
        assertThat(persisted.key()).isEqualTo(original.key());
        assertEntityEquals(persisted, modified);

        var mo = observer.forMethod("update_existingEntity_isRetrievableWithNewValues");
        assertThat(mo.namedEntity(persisted, "persisted").violations()).isEmpty();
    }
}
