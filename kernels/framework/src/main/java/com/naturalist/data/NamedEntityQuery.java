package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;

import java.util.Optional;
import java.util.Set;

/**
 * Query port for a {@link NamedEntity}. The name-only analogue of {@link EntityQuery} —
 * there is no {@code getById} and no {@code findByIdSet}, because a {@link NamedEntity}
 * carries no {@link com.naturalist.ddd.PersistenceId} on its domain record (ADR-021).
 *
 * <p>Single-result lookup returns {@link Optional} per ADR-010. Multi-result lookup
 * returns the domain-specific {@link BehavioralCollection} per ADR-011 — returning a
 * raw {@code List<E>} at a public boundary is a compile error by structure.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <EC>   the behavioral collection type returned by multi-result methods
 */
public interface NamedEntityQuery<NAME extends EntityName<?>, E extends NamedEntity<NAME>, EC extends BehavioralCollection<E>> {

    Optional<E> getByName(NAME name);

    EC findByNameSet(Set<NAME> nameSet);
}
