package com.naturalist.resilience;

import java.util.function.Supplier;

/**
 * Caps the number of concurrent executions allowed. Calls beyond the configured
 * limit either wait up to a maximum queue duration or are rejected immediately,
 * depending on adapter configuration. A rejection surfaces as the kernel
 * {@link BulkheadFullException}, never a vendor type; a permit is released
 * whether the call returns or throws.
 */
public interface Bulkhead {

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
}
