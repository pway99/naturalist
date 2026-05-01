package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Default {@link Resilience} that runs every supplier unprotected. The intended
 * use is unit tests and composition roots that have not yet wired a production
 * adapter — its existence is itself the signal, visible at the call site
 * ({@code Resilience.noOp()}).
 *
 * <p>Production adapters reject unconfigured names by throwing
 * {@link com.naturalist.exception.UnconfiguredResilienceException}; this no-op
 * does not, because its whole purpose is to run unprotected.
 */
final class NoOpResilience implements Resilience {

    static final NoOpResilience INSTANCE = new NoOpResilience();

    private NoOpResilience() {}

    @Override public Retry retry(String name) { return RetryDelegate.INSTANCE; }
    @Override public Timeout timeout(String name) { return TimeoutDelegate.INSTANCE; }
    @Override public CircuitBreaker circuitBreaker(String name) { return CircuitBreakerDelegate.INSTANCE; }
    @Override public Bulkhead bulkhead(String name) { return BulkheadDelegate.INSTANCE; }

    private enum RetryDelegate implements Retry {
        INSTANCE;
        @Override public <T> T execute(Supplier<T> supplier) { return supplier.get(); }
        @Override public void execute(Runnable runnable) { runnable.run(); }
    }

    private enum TimeoutDelegate implements Timeout {
        INSTANCE;
        @Override public <T> T execute(Supplier<T> supplier) { return supplier.get(); }
        @Override public void execute(Runnable runnable) { runnable.run(); }
    }

    private enum CircuitBreakerDelegate implements CircuitBreaker {
        INSTANCE;
        @Override public <T> T execute(Supplier<T> supplier) { return supplier.get(); }
        @Override public void execute(Runnable runnable) { runnable.run(); }
    }

    private enum BulkheadDelegate implements Bulkhead {
        INSTANCE;
        @Override public <T> T execute(Supplier<T> supplier) { return supplier.get(); }
        @Override public void execute(Runnable runnable) { runnable.run(); }
    }
}
