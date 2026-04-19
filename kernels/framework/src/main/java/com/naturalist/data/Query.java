package com.naturalist.data;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

import java.util.Optional;

/**
 * Common contract for any query that resolves a single result by a strongly-typed
 * identifier.
 *
 * <p>Two specialisations extend this:
 * <ul>
 *   <li>{@link EntityQuery} — single-entity lookups, plus multi-result behavioral
 *       collections.</li>
 *   <li>{@link AggregateQuery} — single-aggregate lookups; an {@code AggregateQuery} is
 *       parameterized on the identifier type of its aggregate's <i>root entity</i>, since
 *       in this codebase an aggregate's identity is the identity of its root.</li>
 * </ul>
 *
 * <p>Call sites pass the typed identifier directly via {@link #getById(PersistenceId)} or
 * {@link #getByName(EntityName)}. Both overloads are compile-time type-checked.
 *
 * @param <ID>   the persistence identifier type
 * @param <NAME> the entity name type
 * @param <R>    the result type — an {@link com.naturalist.ddd.Entity} for
 *               {@link EntityQuery}, an {@link com.naturalist.ddd.Aggregate} for
 *               {@link AggregateQuery}
 */
public interface Query<ID extends PersistenceId<?>, NAME extends EntityName<?>, R> {

    Optional<R> getByName(NAME name);

    Optional<R> getById(ID id);
}
