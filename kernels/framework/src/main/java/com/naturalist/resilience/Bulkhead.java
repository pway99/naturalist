package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Caps the number of concurrent executions allowed. Calls beyond the configured
 * limit either wait up to a maximum queue duration or are rejected immediately,
 * depending on adapter configuration.
 */
public interface Bulkhead {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
}
