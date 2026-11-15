package com.naturalist.secrets;

import java.util.Locale;
import java.util.regex.Pattern;
import com.naturalist.observability.Observer;

/**
 * A typed, kebab-case name for a deployment secret, shared by every
 * {@link SecretProvider} source (ADR-028). The kebab form is the file name a
 * file source reads; the upper-snake form is the environment variable an
 * environment source reads.
 *
 * <p>Deliberately not a {@link com.naturalist.ddd.NamedValue}. A {@code NamedValue}
 * may exist in an invalid state and is checked later by its container (ADR-014); a
 * secret name may not. It is a deployment contract fixed in code, and a malformed one
 * can only ever resolve to nothing, so the constructor refuses it (observed, through
 * {@code throwWhenInvalid}) at the moment it is written rather than when a provider
 * first looks it up. It is also never serialised, so it needs none of
 * {@code NamedValue}'s JSON round-trip.
 */
public record SecretName(String value) {
    private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    public SecretName {
        OBSERVER.arguments("constructor", i -> i
                .isTrue(value != null && KEBAB_CASE.matcher(value).matches(), "kebabCase"))
                .throwWhenInvalid();
    }

    public static SecretName of(String value) {
        return new SecretName(value);
    }

    /** The environment variable an environment source reads: {@code example-secret} is {@code EXAMPLE_SECRET}. */
    public String environmentVariable() {
        return value.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    @Override
    public String toString() {
        return value;
    }

    private static final Observer OBSERVER = Observer.forClass(SecretName.class);
}
