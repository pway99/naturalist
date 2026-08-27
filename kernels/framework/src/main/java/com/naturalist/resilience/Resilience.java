package com.naturalist.resilience;

import java.util.Set;

/**
 * Facade over the five resilience primitives the project standardises on.
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
 *
 * <h2>Diagnostics surface</h2>
 * The five {@code *Names()} methods publish the set of strategy names the
 * implementation has registered for each primitive. The admin console reads
 * them to render the {@code /admin/resilience} view; a typo on either the
 * configuration side or the {@code @Resilient(name = ...)} call site is then
 * visible on demand instead of buried in startup logs. The returned sets are
 * unmodifiable.
 */
public interface Resilience {

    Retry retry(String name);

    Timeout timeout(String name);

    CircuitBreaker circuitBreaker(String name);

    Bulkhead bulkhead(String name);

    RateLimiter rateLimiter(String name);

    Set<String> retryNames();

    Set<String> timeoutNames();

    Set<String> circuitBreakerNames();

    Set<String> bulkheadNames();

    Set<String> rateLimiterNames();

    /**
     * A no-op {@code Resilience} that runs every supplier unprotected. Returned
     * primitives execute immediately on the calling thread with no retries, no
     * timeout, no circuit breaker state, and no concurrency cap.
     */
    static Resilience noOp() {
        return NoOpResilience.INSTANCE;
    }
}
