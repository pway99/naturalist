package com.naturalist.ddd;

import com.naturalist.observability.Observable;

/**
 * Common supertype of {@link NamedEntity} and {@link Entity} — any domain record
 * that carries a typed identity. The {@code KEY} type parameter is the identity component.
 *
 * <p>The split between {@link NamedEntity} (slug identity via {@link EntityName}) and
 * {@link Entity} (UUID identity via {@link EntityId}) is a logical distinction at
 * the domain layer. At the repository / test-source layer the two flavors share the same
 * infrastructure — both are keyed on {@code key()} and walked for invariants the same
 * way. Data-layer generics bind on {@code Named<KEY>} so the two branches reuse one
 * implementation.
 *
 * <p>Each branch declares its own semantic accessor — {@code name()} on
 * {@link NamedEntity}, {@code id()} on {@link Entity} — and supplies {@code key()}
 * as a free delegating default.
 */
public interface Named<KEY> extends Observable {
    KEY key();
}
