package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.Retry;

import java.util.function.Supplier;

final class Resilience4jRetry implements Retry {

    private final io.github.resilience4j.retry.Retry delegate;

    Resilience4jRetry(io.github.resilience4j.retry.Retry delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        return delegate.executeSupplier(supplier);
    }

    @Override
    public void execute(Runnable runnable) {
        delegate.executeRunnable(runnable);
    }
}
