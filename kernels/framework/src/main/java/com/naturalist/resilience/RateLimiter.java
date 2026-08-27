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
 */
public interface RateLimiter {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
}
