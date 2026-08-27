package com.naturalist.resilience;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by {@link RateLimiter#execute} when a permit cannot be acquired for
 * the configured strategy. Unchecked — a rate rejection is an expected,
 * caller-handled outcome (redirect to a friendly "try again later" page), not
 * a programming error, but it should not be silently swallowed either.
 *
 * <p>Kernel-owned so that domain and console code can catch a single type
 * regardless of which resilience adapter is wired — the production
 * Resilience4j bridge adapter translates its vendor rejection into this
 * type; no vendor exception ever needs to cross an adapter boundary.
 */
public class RateLimitExceededException extends RuntimeException {

    private final @Nullable String strategyName;

    public RateLimitExceededException(String message) {
        this(message, null);
    }

    public RateLimitExceededException(String message, @Nullable String strategyName) {
        super(message);
        this.strategyName = strategyName;
    }

    public @Nullable String strategyName() {
        return strategyName;
    }
}
