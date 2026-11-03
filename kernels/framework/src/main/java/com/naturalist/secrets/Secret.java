package com.naturalist.secrets;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A deployment secret's value (ADR-028). {@link #toString()} redacts it to
 * {@code Secret[<fingerprint>]}, and {@link #fingerprint()} is the first eight
 * hex digits of its SHA-256, safe to log and display so an operator can prove
 * which secret is live without seeing it.
 *
 * <p>{@link #value()} is for an adapter at construction, to hand to its
 * client. Nothing else may hold the string.
 */
public final class Secret {
    private final String value;

    private Secret(String value) {
        this.value = value;
    }

    public static Secret of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("secret value must not be blank");
        }
        return new Secret(value);
    }

    /** The raw value, for a vendor adapter at construction only. */
    public String value() {
        return value;
    }

    /** The first eight lowercase hex digits of the value's SHA-256. */
    public String fingerprint() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Secret that
                && MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8), that.value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "Secret[" + fingerprint() + "]";
    }
}
