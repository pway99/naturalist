package com.naturalist.ddd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.naturalist.observability.constraints.EmailFormatConstraint;

import java.util.Locale;
import java.util.function.Function;

/**
 * A typed email address — the recipient of an outbound {@code EmailMessage} and, in time, the
 * login identity the {@code accounts} domain keys on. A {@link NamedValue}{@code <String>} per
 * ADR-014: a distinct type rather than a raw {@code String}, so an address can never be
 * handed to a sender in a subject's or a handle's place, and so the format rule lives in one
 * spot instead of being re-asserted at every call site. Lives beside {@link NamedValue} rather
 * than in a feature package because it is cross-cutting and because
 * {@code Constraints#emailAddress} must name it without {@code observability} depending on a
 * feature.
 *
 * <p>Containers validate it with {@code Constraints#emailAddress(EmailAddress, String)}, which
 * observes an {@link EmailFormatConstraint} directly — so the metric tag reads
 * {@code constraint=EmailFormatConstraint}, the same vocabulary a raw-string
 * {@code Constraints#email} check emits, rather than the opaque {@code NamedValueConstraint}
 * the generic {@code namedValue(...)} path would record. {@link #isValid()} delegates to that
 * same constraint so both paths agree (one {@code @}, a dotted domain, no whitespace, at most
 * 254 characters). Per ADR-014 the constructor never throws: an invalid address can exist, and
 * the owning record's {@code invariants()} surfaces it at the boundary.
 *
 * <p>{@code @JsonValue} on {@link #value()} serialises as a plain string and
 * {@code @JsonCreator} on {@link #of(String)} reads one back, so JSON carries
 * {@code "to": "ada@example.com"}, never an envelope object.
 */
public record EmailAddress(String value) implements NamedValue<String> {
    /**
     * The canonical address for {@code value}: stripped of surrounding whitespace and
     * lower-cased. Email lookups are exact-match ({@code WHERE email = ?}), so without one
     * canonical form {@code Ada@Example.com} and {@code ada@example.com} are two accounts, a
     * login must repeat the registration's casing, and duplicate detection is bypassed by a
     * capital letter. Lower-casing the local part too is a deliberate, conventional
     * over-approximation (RFC 5321 allows case-sensitive local parts; no real provider does).
     */
    @JsonCreator
    public static EmailAddress of(String value) {
        return new EmailAddress(value == null ? null : value.strip().toLowerCase(Locale.ROOT));
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean isValid() {
        return new EmailFormatConstraint<>(value, Function.<String>identity(), "value").isValid();
    }
}
