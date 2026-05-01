package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.Timeout;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

final class Resilience4jTimeout implements Timeout {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    private final TimeLimiter delegate;

    Resilience4jTimeout(TimeLimiter delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        try {
            return delegate.executeFutureSupplier(() -> CompletableFuture.supplyAsync(supplier, SCHEDULER));
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
