package com.naturalist.resilience;

/**
 * Facade over the four resilience primitives the project standardises on.
 * Domain {@code *-core} code references this interface only — never a concrete
 * Resilience4j (or other vendor) type. The default {@link #noOp()} runs every
 * supplier unprotected and is suitable for unit tests and composition roots
 * that have not yet wired a production adapter.
 *
 * <p>An app obtains a production implementation from {@code adapters/resilience-resilience4j/};
 * domain code receives whichever {@code Resilience} the composition root supplies.
 *
 * <h2>Primitive lookup</h2>
 * Each primitive method takes a {@code name} that selects a configuration
 * registered with the implementation. Names that have no matching configuration
 * fall back to a no-op for that primitive — strict configuration validation is
 * the composition root's job, not a runtime concern at every call site.
 */
public interface Resilience {

    Retry retry(String name);

    Timeout timeout(String name);

    CircuitBreaker circuitBreaker(String name);

    Bulkhead bulkhead(String name);

    /**
     * A no-op {@code Resilience} that runs every supplier unprotected. Returned
     * primitives execute immediately on the calling thread with no retries, no
     * timeout, no circuit breaker state, and no concurrency cap.
     */
    static Resilience noOp() {
        return NoOpResilience.INSTANCE;
    }
}
