package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Trips open after a configured failure rate or slow-call rate, short-circuiting
 * subsequent calls until a half-open trial succeeds. While open, the breaker
 * rejects calls without invoking the supplier — callers see the adapter's
 * runtime rejection exception.
 */
public interface CircuitBreaker {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
}
