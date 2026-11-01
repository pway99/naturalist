package com.naturalist.notification;

import com.naturalist.ddd.EmailAddress;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A plain-text outbound email: a {@code to} recipient, a {@code subject}, and a {@code body}.
 * Deliberately minimal — the flows that send mail today (email-verification and
 * password-reset links, usage alerts) are plain text. Richer content (HTML bodies, multiple
 * recipients, a reply-to) is added when a flow actually needs it, not speculatively.
 *
 * <p>A {@link ValueObject} like the framework's other infrastructure values ({@code Page},
 * {@code PageRequest}): identity-free, equal by attributes, and validated through
 * {@link #invariants()} so an {@link EmailSender} can observe its argument at the boundary
 * and reject a malformed message with an {@code InvariantViolationException} — the same
 * path every command and query in the system takes, never a bare {@code NullPointerException}.
 * The recipient is a typed {@link EmailAddress} (ADR-014), which carries its own
 * address-format validity; this record only asserts that it is present and valid.
 */
public record EmailMessage(EmailAddress to, String subject, String body) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return (Constraints c) -> c
                .emailAddress(to, "to")
                .notBlank(subject, "subject")
                .notBlank(body, "body");
    }
}
