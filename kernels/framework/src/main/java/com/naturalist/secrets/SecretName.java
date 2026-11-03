package com.naturalist.secrets;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A typed, kebab-case name for a deployment secret, shared by every
 * {@link SecretProvider} source (ADR-028). The kebab form is the file name a
 * file source reads; the upper-snake form is the environment variable an
 * environment source reads.
 */
public record SecretName(String value) {
    private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    public SecretName {
        if (value == null || !KEBAB_CASE.matcher(value).matches()) {
            throw new IllegalArgumentException("secret name must be kebab-case: '" + value + "'");
        }
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
}
