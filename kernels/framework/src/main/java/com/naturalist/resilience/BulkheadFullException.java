package com.naturalist.resilience;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by {@link Bulkhead#execute} when no concurrency permit is free for
 * the configured strategy. Unchecked — a full bulkhead is an expected,
 * caller-handled outcome (a friendly "busy, try again" answer), not a
 * programming error, but it should not be silently swallowed either.
 *
 * <p>Kernel-owned so that domain and console code can catch a single type
 * regardless of which resilience adapter is wired — the production
 * Resilience4j bridge adapter translates its vendor rejection into this
 * type; no vendor exception ever needs to cross an adapter boundary.
 */
public class BulkheadFullException extends RuntimeException {
    private final @Nullable String strategyName;

    public BulkheadFullException(String message) {
        this(message, null);
    }

    public BulkheadFullException(String message, @Nullable String strategyName) {
        super(message);
        this.strategyName = strategyName;
    }

    public @Nullable String strategyName() {
        return strategyName;
    }
}
