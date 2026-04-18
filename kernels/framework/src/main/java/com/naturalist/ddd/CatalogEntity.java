package com.naturalist.ddd;

/**
 * A stable, named classification that exists independently of observation.
 * <p>
 * A {@code CatalogEntity} has a meaningful natural key — an {@link CatalogName} slug that
 * the domain already knows. This slug is used for cross-domain references (per ADR-001)
 * and is never derived from the entity's other fields.
 * <p>
 * The non-nullable {@code name()} override makes the natural key contract explicit in
 * the type system. Every {@code CatalogEntity} record declares a {@code name} component
 * (or an explicit {@code name()} override) that returns a non-null {@link CatalogName}.
 * <p>
 * Examples: {@code Element}, {@code CompoundInfo}, {@code InsectSpecies}, {@code Plant},
 * {@code ZoneInfo}, {@code SoilProfileInfo}
 *
 * @see FactEntity
 * @see ADR-005
 */
public interface CatalogEntity<ID extends PersistenceId<?>, NAME extends CatalogName> extends Entity<ID, NAME> {

}
