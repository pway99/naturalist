package com.naturalist.ddd;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A typed, non-identifying, single-value wrapper whose type carries its own domain meaning.
 * <p>
 * A {@code NamedValue<T>} solves the primitive escape problem: a raw {@code String} or
 * {@code double} loses its domain context the moment it leaves its container. A
 * {@code NamedValue<T>} implementation — for example {@code TaxonomicOrder} — carries its
 * meaning structurally. A method accepting {@code TaxonomicOrder} cannot accidentally
 * receive a {@code TaxonomicFamily}; both are {@code NamedValue<String>} but the compiler
 * treats them as distinct types.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #value()} returns the wrapped domain value, which may be {@code null} if the
 *       instance was constructed from absent data.</li>
 *   <li>{@link #isValid()} is the sole validation predicate. A {@code NamedValue<T>} may
 *       exist in an invalid state — no constructor should throw to prevent it. Invalidity is
 *       surfaced at domain boundaries by {@code Constraints#namedValue(...)}, consistent with
 *       how {@link EntityName} fields are validated.</li>
 *   <li>{@link #isNotValid()} is the negation convenience; do not override it.</li>
 * </ul>
 *
 * <h2>Relationship to EntityName</h2>
 * {@link EntityName} is a natural key — it implies uniqueness and participates in entity
 * lookup contracts. {@code NamedValue<T>} makes no uniqueness claim and never appears as
 * a repository query parameter.
 *
 * <h2>Relationship to ValueObject</h2>
 * A {@link ValueObject} is a multi-field composite where members have collective meaning as
 * a domain concept. A {@code NamedValue<T>} wraps a single value and is not {@code Observable}.
 * The container that owns the field declares the invariant; the named value does not.
 *
 * <h2>Decimal quantities</h2>
 * Domain fields representing decimal measurements must use {@link NumericNamedValue}, which
 * extends this interface with a required {@code scale()} and {@code roundingMode()}.
 * Raw {@code double} and {@code float} are prohibited for decimal domain values.
 *
 * <h2>Implementation pattern</h2>
 * Concrete implementations are records. No constructor logic; validation belongs in
 * {@link #isValid()} only.
 * <pre>{@code
 * public record TaxonomicOrder(String value) implements NamedValue<String> {
 *
 *     public static TaxonomicOrder of(String value) {
 *         return new TaxonomicOrder(value);
 *     }
 *
 *     @Override
 *     public boolean isValid() {
 *         return value != null && !value.isBlank();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of the wrapped value
 * @see NumericNamedValue
 * @see EntityName
 * @see ValueObject
 */
public interface NamedValue<T> {
    @JsonValue
    T value();

    boolean isValid();

    default boolean isNotValid() {
        return !isValid();
    }
}
