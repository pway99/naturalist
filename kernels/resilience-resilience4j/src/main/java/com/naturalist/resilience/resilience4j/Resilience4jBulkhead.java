package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.Bulkhead;

import java.util.function.Supplier;

final class Resilience4jBulkhead implements Bulkhead {

    private final io.github.resilience4j.bulkhead.Bulkhead delegate;

    Resilience4jBulkhead(io.github.resilience4j.bulkhead.Bulkhead delegate) {
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
