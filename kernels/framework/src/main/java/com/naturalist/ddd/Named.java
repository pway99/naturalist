package com.naturalist.ddd;

import com.naturalist.observability.Observable;

/**
 * Common supertype of {@link NamedEntity} and {@link Entity} — any domain record
 * that carries a typed name. The {@code NAME} type parameter is the identity component.
 *
 * <p>The split between {@link NamedEntity} (slug identity via {@link EntityName}) and
 * {@link Entity} (UUID identity via {@link EntityId}) is a logical distinction at
 * the domain layer. At the repository / test-source layer the two flavors share the same
 * infrastructure — both are keyed on {@code name()} and walked for invariants the same
 * way. Data-layer generics bind on {@code Named<NAME>} so the two branches reuse one
 * implementation.
 */
public interface Named<NAME> extends Observable {
    NAME name();
}
