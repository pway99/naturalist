package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.Timeout;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

final class Resilience4jTimeout implements Timeout {

    /**
     * One virtual thread per timed call, so strategies never queue behind each other: a worker
     * keeps running after its caller times out, and on a fixed pool those orphans would spend
     * other strategies' deadlines. {@link #execute(Supplier)} blocks its caller, so concurrency
     * stays bounded by the callers themselves; a cap on them is what a bulkhead or a rate limiter
     * is for.
     */
    private static final ExecutorService WORKERS = Executors.newVirtualThreadPerTaskExecutor();

    private final TimeLimiter delegate;

    Resilience4jTimeout(TimeLimiter delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        try {
            return delegate.executeFutureSupplier(() -> CompletableFuture.supplyAsync(supplier, WORKERS));
        } catch (TimeoutException e) {
            throw new ResilienceTimeoutException(delegate.getName(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(Runnable runnable) {
        execute(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Wall-clock duration exceeded the configured limit. Distinguishable from
     * the underlying {@link TimeoutException}; lets callers handle adapter
     * timeouts without catching the broad checked exception.
     */
    public static final class ResilienceTimeoutException extends RuntimeException {
        ResilienceTimeoutException(String name, Throwable cause) {
            super("Timeout '" + name + "' exceeded its configured duration", cause);
        }
    }
}
