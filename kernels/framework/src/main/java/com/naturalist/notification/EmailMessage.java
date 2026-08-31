package com.naturalist.notification;

import java.util.Objects;

/**
 * A plain-text outbound email: a {@code to} recipient, a {@code subject}, and a {@code body}.
 * Deliberately minimal — the flows that send mail today (email-verification and
 * password-reset links, usage alerts) are plain text. Richer content (HTML bodies, multiple
 * recipients, a reply-to) is added when a flow actually needs it, not speculatively.
 *
 * <p>This is an infrastructure carrier, not a domain type: it enforces only non-nullness,
 * leaving address-format validation to the domain that already owns it (the {@code accounts}
 * email constraint), so the port never second-guesses its callers.
 */
public record EmailMessage(String to, String subject, String body) {

    public EmailMessage {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
    }
}
