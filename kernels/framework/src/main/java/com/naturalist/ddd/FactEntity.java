package com.naturalist.ddd;

/**
 * A record of a singular, immutable occurrence — something that happened or was measured
 * at a specific point in time.
 * <p>
 * A {@code FactEntity} satisfies Eric Evans' Entity definition — it has a
 * {@link PersistenceId} and exists over time as an immutable fact — but it has no
 * meaningful natural key. Its identity is temporal and contextual. It is never
 * referenced cross-domain by slug. Other domains that need to reason about facts
 * do so through service interfaces, never through direct slug references.
 * <p>
 * Two sub-kinds naturally emerge, though both share the same identity model:
 * <ul>
 *   <li><b>Events</b> — things done by the naturalist: {@code AmendmentEvent},
 *       {@code IrrigationEvent}, {@code TillageEvent}. Human-initiated, discrete,
 *       causally significant.</li>
 *   <li><b>Observations</b> — things measured or noticed: {@code LabAnalysis},
 *       sensor readings, species sightings, phenological records. Passive records
 *       of state in nature.</li>
 * </ul>
 * Both are immutable. The distinction is a query and domain concern, not an
 * identity concern.
 * <p>
 * {@code PersistenceId} is the sole identity. There is no {@link CatalogName}.
 * The {@link Entity#name()} default returns {@code null} for all fact entities.
 * <p>
 * <b>Boundary with domain laws and constants:</b> a generalisation derived from
 * observations — a physical law, a chemical constant, an agronomic rule — is not
 * a {@code FactEntity}. The measurement that produced the constant is a fact;
 * the constant itself is domain knowledge, encoded as a {@link ValueObject} or
 * a pure Java constant. It requires no persistence key and no RDBMS table.
 * See ADR-005 §"Domain Laws and Constants".
 *
 * @see CatalogEntity
 * @see ValueObject
 */
public interface FactEntity<ID extends PersistenceId<?>, NAME extends FactName> extends Entity<ID, NAME> {
}
