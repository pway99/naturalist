package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Asserts that a string-typed value is a plausibly well-formed email address:
 * exactly one {@code @}, a non-empty local part, and a dotted domain — with no
 * whitespace anywhere — and no longer than {@value #MAX_LENGTH} characters
 * (the RFC 5321 ceiling).
 * <p>
 * This is a deliberately pragmatic sanity check, not RFC-perfect validation.
 * The authoritative proof that an address is usable is that a message sent to
 * it is delivered; this constraint only rejects the obvious garbage a login
 * field should never accept. Normalisation (lower-casing, trimming) is a
 * command-layer concern, not an invariant.
 * <p>
 * Null fails and empty fails (the {@link #errorMessage()} distinguishes the
 * two), so a single {@code email(...)} constraint covers presence and format.
 *
 * @param <T> the carrier type — typically the enclosing record or class
 */
public record EmailFormatConstraint<T>(
        T o,
        Function<T, String> valueFunction,
        String name
) implements Constraint<String> {

    /** RFC 5321 maximum total length of an email address. */
    static final int MAX_LENGTH = 254;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    public String value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        if (o == null) return false;
        String v = valueFunction.apply(o);
        return v != null && v.length() <= MAX_LENGTH && EMAIL.matcher(v).matches();
    }

    @Override
    public EmailFormatConstraint<T> withName(String name) {
        return new EmailFormatConstraint<>(o, valueFunction, name);
    }

    @Override
    public String errorMessage() {
        if (o == null) return "null carrier";
        String v = valueFunction.apply(o);
        if (v == null) return "null value";
        if (v.isEmpty()) return "empty value";
        if (v.length() > MAX_LENGTH) {
            return "Length %d exceeds max %d".formatted(v.length(), MAX_LENGTH);
        }
        return "Value '%s' is not a valid email address".formatted(v);
    }
}
