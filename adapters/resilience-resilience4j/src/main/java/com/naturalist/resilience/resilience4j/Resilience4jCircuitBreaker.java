package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.CircuitBreaker;

import java.util.function.Supplier;

final class Resilience4jCircuitBreaker implements CircuitBreaker {

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    Resilience4jCircuitBreaker(io.github.resilience4j.circuitbreaker.CircuitBreaker delegate) {
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
