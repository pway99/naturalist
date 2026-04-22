package com.naturalist.ddd;

import com.naturalist.observability.Observable;

/**
 * An immutable, identity-free value defined entirely by its attributes.
 * <p>
 * Two {@code ValueObject} instances with identical attribute values are interchangeable.
 * They carry no surrogate key, no natural slug, and require no RDBMS persistence unless
 * embedded within a persistent entity.
 * <p>
 * {@code ValueObject} covers two distinct roles in this domain:
 * <ul>
 *   <li><b>Descriptive values</b> — measurements, profiles, and structured data that
 *       belong to an entity but have no identity of their own: {@code SafetyProfile},
 *       {@code SolubilityProfile}, {@code Description}.</li>
 *   <li><b>Domain laws and constants</b> — scientific laws, physical constants, and
 *       agronomic rules codified in pure Java. These are generalisations derived from
 *       observation and held as true until the model is corrected. They are not
 *       {@link Entity} instances — the measurement that produced a constant is a
 *       fact; the constant itself is domain knowledge. Examples: a gravitational field
 *       value, an optimal nutrient range, a chemical binding constant.</li>
 * </ul>
 * Domain laws and constants that are universal and dimensionless within the domain may
 * also be expressed as {@code static final} constants on a domain class rather than as
 * {@code ValueObject} records — the choice is a readability concern, not an identity one.
 * Neither form requires an RDBMS table.
 *
 * @see Entity
 * @see NamedEntity
 */
public interface ValueObject extends Observable {
}
