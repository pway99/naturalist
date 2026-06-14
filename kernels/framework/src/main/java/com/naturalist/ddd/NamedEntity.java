package com.naturalist.ddd;

/**
 * An entity whose identity is its {@link EntityName} — a stable natural-key slug.
 * The RDBMS adapter for a named entity may carry a numeric primary key in its schema,
 * but that key lives entirely inside the adapter — it never surfaces on the domain
 * record (ADR-021).
 *
 * <p>Cross-entity references are expressed as an {@link EntityName}. There is no
 * {@code id()}, no {@code withId(...)} — identity is the name.
 *
 * @param <NAME> the entity's name type
 */
public interface NamedEntity<NAME extends EntityName> extends Named<NAME> {
    NAME name();

    @Override
    default NAME key() {
        return name();
    }
}
