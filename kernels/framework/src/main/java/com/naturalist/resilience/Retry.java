package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Re-executes a failing call up to a configured maximum, with optional backoff
 * between attempts. Adapter implementations decide which exception types are
 * retryable; the default in production adapters is "every checked and unchecked
 * exception except those explicitly excluded by configuration."
 */
public interface Retry {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
}
