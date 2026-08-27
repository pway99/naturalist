package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.RateLimitExceededException;
import com.naturalist.resilience.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import java.util.function.Supplier;

final class Resilience4jRateLimiter implements RateLimiter {

    private final io.github.resilience4j.ratelimiter.RateLimiter delegate;

    Resilience4jRateLimiter(io.github.resilience4j.ratelimiter.RateLimiter delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        try {
            return delegate.executeSupplier(supplier);
        } catch (RequestNotPermitted e) {
            throw rejected();
        }
    }

    @Override
    public void execute(Runnable runnable) {
        try {
            delegate.executeRunnable(runnable);
        } catch (RequestNotPermitted e) {
            throw rejected();
        }
    }

    /**
     * Translates the vendor rejection into the kernel exception — the vendor
     * type {@link RequestNotPermitted} must never escape this adapter.
     */
    private RateLimitExceededException rejected() {
        return new RateLimitExceededException(
                "Rate limit exceeded for strategy '" + delegate.getName() + "'",
                delegate.getName());
    }
}
