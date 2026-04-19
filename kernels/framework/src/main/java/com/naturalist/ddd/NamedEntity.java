package com.naturalist.ddd;

import com.naturalist.observability.Observable;

/**
 * An entity whose identity is its {@link EntityName} alone — no separate
 * {@link PersistenceId} component on the domain record. Adopted by domains that have
 * migrated to ADR-021 (persistence-id is adapter-internal).
 *
 * <p>Cross-entity references from a {@code NamedEntity} are always expressed as an
 * {@link EntityName}. The RDBMS adapter for such a domain is free to carry a numeric
 * primary key in its schema and use numeric foreign keys for joins, but those keys
 * live entirely inside the adapter — they never surface on the domain record.
 *
 * <p>This interface is parallel to {@link Entity} rather than a refinement of it. The
 * two coexist: non-migrated domains continue to implement {@code Entity<ID, NAME>};
 * migrated domains implement {@code NamedEntity<NAME>}. No {@code id()}, no
 * {@code withId(...)} — identity is the name.
 *
 * @param <NAME> the entity's name type
 * @see Entity
 * @see EntityName
 */
public interface NamedEntity<NAME extends EntityName<?>> extends Observable {

    /**
     * The stable natural key for this entity. Never null.
     */
    NAME name();
}
