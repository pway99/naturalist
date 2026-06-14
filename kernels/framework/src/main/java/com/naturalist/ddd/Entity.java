package com.naturalist.ddd;

/**
 * A domain record identified by a surrogate {@link EntityId} (UUIDv7).
 *
 * <p>Use {@code Entity} for records whose identity is not a natural key —
 * events, observations, measurements, lab analyses. The identifier is a
 * time-ordered UUIDv7 generated at record construction, so the entire identity
 * lifecycle begins in Java and no round-trip to the database is required at
 * insert time.
 *
 * <p>Cross-domain references to {@code Entity} instances are never by value.
 * Other domains reason about them through service interfaces.
 *
 * @param <ID> the concrete {@link EntityId} subtype for this entity
 */
public interface Entity<ID extends EntityId> extends Named<ID> {
    ID id();

    @Override
    default ID key() {
        return id();
    }
}
