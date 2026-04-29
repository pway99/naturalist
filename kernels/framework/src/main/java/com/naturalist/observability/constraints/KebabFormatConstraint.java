package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Asserts that a string-typed value matches the project's standard kebab-case
 * slug format: one or more lowercase-alphanumeric segments separated by single
 * hyphens, with no leading, trailing, or consecutive hyphens.
 * <p>
 * This is the same format every {@link com.naturalist.ddd.EntityName} subclass
 * applies to its own {@code value} string. Factor it out as a reusable
 * {@link Constraint} so any value object that wraps a kebab slug — a
 * {@code DomainId}, a future {@code Tag}, an arbitrary user-facing slug field —
 * can express the same rule without re-deriving the regex.
 * <p>
 * Null fails (use the existing notNull/notBlank constraints to express
 * presence separately if a more specific message is desired). Empty fails.
 *
 * @param <T> the carrier type — typically the enclosing record or class
 */
public record KebabFormatConstraint<T>(
        T o,
        Function<T, String> valueFunction,
        String name
) implements Constraint<String> {

    private static final Pattern LOWER_KEBAB = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    @Override
    public String value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        if (o == null) return false;
        String v = valueFunction.apply(o);
        return v != null && LOWER_KEBAB.matcher(v).matches();
    }

    @Override
    public KebabFormatConstraint<T> withName(String name) {
        return new KebabFormatConstraint<>(o, valueFunction, name);
    }

    @Override
    public String errorMessage() {
        if (o == null) return "null carrier";
        String v = valueFunction.apply(o);
        if (v == null) return "null value";
        if (v.isEmpty()) return "empty value";
        return "Value '%s' is not lower-kebab-case".formatted(v);
    }
}
