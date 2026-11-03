package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Caps the rate at which calls proceed within a rolling window. A call beyond
 * the configured limit either waits (up to the configured wait timeout) or is
 * rejected.
 *
 * <p>{@link #execute(Supplier)} and {@link #execute(Runnable)} throw
 * {@link RateLimitExceededException} when no permit can be acquired within
 * the configured wait timeout. The exception is kernel-owned — adapters
 * translate their own vendor rejection into it rather than letting it
 * escape.
 *
 * <p>{@link #forKey(String)} narrows the budget to one caller: the same configuration,
 * applied per key (a remote address, an account) rather than system-wide. A keyed
 * limiter and the unkeyed one it came from are independent, so a per-client ceiling
 * normally sits <em>beside</em> a system-wide one, not instead of it — adapters bound
 * the set of live keys and may evict the least-recently-used, which resets that key's
 * budget, and only the unkeyed limiter catches a caller who rotates keys.
 */
public interface RateLimiter {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);

    /**
     * This strategy's budget for one {@code key}. Keys are opaque to the kernel and chosen
     * by the caller; they are never put in a metric tag (unbounded cardinality) — log them.
     * Calling {@code forKey} on a limiter that is already keyed returns that limiter.
     */
    RateLimiter forKey(String key);
}
