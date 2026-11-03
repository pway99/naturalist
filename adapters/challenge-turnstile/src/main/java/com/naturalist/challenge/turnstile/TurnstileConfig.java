package com.naturalist.challenge.turnstile;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.function.Consumer;

/**
 * Where to verify and what to expect back. The endpoint is an input so tests point it at a
 * local stub; production uses {@link #of(String)}. A blank {@code expectedHostname} skips the
 * hostname check and leaves a standing violation, so it cannot go unnoticed. The credential
 * comes from the kernel {@code SecretProvider}, not from this config (ADR-028).
 */
public record TurnstileConfig(@Nullable String expectedHostname, URI endpoint) implements ValueObject {

    private static final URI SITEVERIFY = URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify");

    public static TurnstileConfig of(@Nullable String expectedHostname) {
        return new TurnstileConfig(expectedHostname, SITEVERIFY);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return (Constraints c) -> c.notNull(endpoint, "endpoint");
    }
}
