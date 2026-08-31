package com.naturalist.console.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Property-bound email settings under {@code naturalist.email.*}. {@code from} is the
 * envelope sender used on outbound mail (verification / reset links, usage alerts).
 * Defaults to a {@code naturalist.local} no-reply address when unset — good enough for the
 * log-only dev sender, overridden per deployment once a real sending domain is configured.
 */
@ConfigurationProperties("naturalist.email")
public record EmailProperties(String from) {

    public EmailProperties {
        if (from == null || from.isBlank()) {
            from = "no-reply@naturalist.local";
        }
    }
}
