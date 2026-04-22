package com.naturalist.ddd;

/**
 * A record of a singular, immutable occurrence — something that happened or was measured
 * at a specific point in time.
 *
 * <p>A {@code FactEntity}'s identity is a surrogate {@link FactName} (UUID), not a natural
 * key. It is never referenced cross-domain by value; other domains reason about facts
 * through service interfaces.
 *
 * <p>Two sub-kinds share the same identity model:
 * <ul>
 *   <li><b>Events</b> — things done by the naturalist: {@code AmendmentEvent},
 *       {@code IrrigationEvent}, {@code TillageEvent}. Human-initiated, discrete,
 *       causally significant.</li>
 *   <li><b>Observations</b> — things measured or noticed: {@code LabAnalysis},
 *       sensor readings, species sightings, phenological records.</li>
 * </ul>
 *
 * <p><b>Boundary with domain laws and constants:</b> a generalisation derived from
 * observations — a physical law, a chemical constant, an agronomic rule — is not
 * a {@code FactEntity}. The measurement that produced the constant is a fact; the
 * constant itself is domain knowledge, encoded as a {@link ValueObject} or a pure
 * Java constant.
 */
public interface FactEntity<NAME extends FactName> extends Named<NAME> {
}
