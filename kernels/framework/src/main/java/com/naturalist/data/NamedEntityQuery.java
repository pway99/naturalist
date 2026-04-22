package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Named;

import java.util.Optional;
import java.util.Set;

/**
 * Query port for a {@link Named} entity — any domain record carrying a typed name.
 * Identity at the port is the entity's name (ADR-021) — there is no id-based lookup
 * because domain records carry no persistence identifier.
 *
 * <p>Single-result lookup returns {@link Optional} per ADR-010. Multi-result lookup
 * returns the domain-specific {@link BehavioralCollection} per ADR-011 — returning a
 * raw {@code List<E>} at a public boundary is a compile error by structure.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <EC>   the behavioral collection type returned by multi-result methods
 */
public interface NamedEntityQuery<NAME, E extends Named<NAME>, EC extends BehavioralCollection<E>> {

    Optional<E> getByName(NAME name);

    EC findByNameSet(Set<NAME> nameSet);
}
