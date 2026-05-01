package com.naturalist.resilience;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Value-object configuration for one named resilience primitive. Adapters
 * accept a {@code List<ResilienceConfig>} at assembly time and build the
 * primitive registries from it; nothing here references a specific vendor.
 *
 * <p>The same {@code name} may appear in multiple records of different types —
 * a single named strategy ("catalog.fanout") can combine retry, timeout, and
 * circuit-breaker configuration. Callers reference the strategy by that shared
 * name via {@link Resilient}.
 *
 * <p>Each variant declares {@link #invariants()} so the composition root can
 * walk the constraint graph at assembly time and reject an invalid bundle
 * before the application starts taking traffic.
 */
public sealed interface ResilienceConfig extends Observable {

    String name();

    record RetryConfig(String name, int maxAttempts, Duration backoff)
            implements ResilienceConfig {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .notNull(backoff, "backoff");
        }
    }

    record TimeoutConfig(String name, Duration duration)
            implements ResilienceConfig {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .notNull(duration, "duration");
        }
    }

    record CircuitBreakerConfig(String name,
                                float failureRateThreshold,
                                Duration slowCallDurationThreshold,
                                float slowCallRateThreshold,
                                Duration waitDurationInOpenState,
                                int minimumNumberOfCalls)
            implements ResilienceConfig {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .notNull(slowCallDurationThreshold, "slowCallDurationThreshold")
                    .notNull(waitDurationInOpenState, "waitDurationInOpenState");
        }
    }

    record BulkheadConfig(String name, int maxConcurrentCalls, Duration maxWaitDuration)
            implements ResilienceConfig {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .notNull(maxWaitDuration, "maxWaitDuration");
        }
    }
}
