package com.naturalist.challenge.turnstile;

import com.naturalist.resilience.Bulkhead;
import com.naturalist.resilience.CircuitBreaker;
import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Retry;
import com.naturalist.resilience.Timeout;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Just enough of the {@link Resilience} facade to prove the adapter's nesting and what each
 * layer sees. An adapter may not depend on another adapter, so the vendor implementation is
 * not on this classpath. Each primitive behaves as the facade's Javadoc promises: the retry
 * retries every exception, the timeout runs the supplier on a worker and bounds it, and the
 * breaker counts what escapes it and then refuses without invoking the supplier.
 */
final class TestResilience implements Resilience {

    private final int attempts;
    private final Duration ceiling;
    private final int breakAfter;

    private TestResilience(int attempts, Duration ceiling, int breakAfter) {
        this.attempts = attempts;
        this.ceiling = ceiling;
        this.breakAfter = breakAfter;
    }

    static TestResilience retrying(int attempts) {
        return new TestResilience(attempts, null, Integer.MAX_VALUE);
    }

    static TestResilience bounded(Duration ceiling, int attempts) {
        return new TestResilience(attempts, ceiling, Integer.MAX_VALUE);
    }

    static TestResilience breakingAfter(int failures) {
        return new TestResilience(1, null, failures);
    }

    @Override
    public Retry retry(String name) {
        return new Retry() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                RuntimeException last = null;
                for (int i = 0; i < attempts; i++) {
                    try {
                        return supplier.get();
                    } catch (RuntimeException e) {
                        last = e;
                    }
                }
                throw last;
            }

            @Override
            public void execute(Runnable runnable) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Timeout timeout(String name) {
        return new Timeout() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                if (ceiling == null) {
                    return supplier.get();
                }
                try (var worker = Executors.newVirtualThreadPerTaskExecutor()) {
                    var future = worker.submit(supplier::get);
                    try {
                        return future.get(ceiling.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        future.cancel(true);
                        throw new IllegalStateException("timed out", e);
                    } catch (ExecutionException e) {
                        throw new IllegalStateException(e.getCause());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }
            }

            @Override
            public void execute(Runnable runnable) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private final AtomicInteger failures = new AtomicInteger();

    @Override
    public CircuitBreaker circuitBreaker(String name) {
        return new CircuitBreaker() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                if (failures.get() >= breakAfter) {
                    throw new IllegalStateException("breaker open");
                }
                try {
                    return supplier.get();
                } catch (RuntimeException e) {
                    failures.incrementAndGet();
                    throw e;
                }
            }

            @Override
            public void execute(Runnable runnable) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Bulkhead bulkhead(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RateLimiter rateLimiter(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> retryNames() {
        return Set.of();
    }

    @Override
    public Set<String> timeoutNames() {
        return Set.of();
    }

    @Override
    public Set<String> circuitBreakerNames() {
        return Set.of();
    }

    @Override
    public Set<String> bulkheadNames() {
        return Set.of();
    }

    @Override
    public Set<String> rateLimiterNames() {
        return Set.of();
    }
}
