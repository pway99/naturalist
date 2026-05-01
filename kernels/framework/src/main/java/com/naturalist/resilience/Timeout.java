package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Cancels a call that exceeds a configured wall-clock duration. The supplier
 * runs on a worker thread; on timeout the calling thread receives a
 * {@link java.util.concurrent.TimeoutException} wrapped in the adapter's
 * runtime exception.
 */
public interface Timeout {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
}
