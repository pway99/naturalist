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
 * before the application starts taking traffic. Range bounds catch the kind
 * of typo that produces a one-day timeout or a zero retry count — defects
 * that would otherwise surface as production outages.
 */
public sealed interface ResilienceConfig extends Observable {

    /** Hard ceiling for a synchronous request-path timeout. Anything longer
     *  blocks a user-facing thread past the point where retries or a polite
     *  error response would be the better answer — those calls belong on an
     *  async worker, not in the request path. */
    Duration MAX_TIMEOUT = Duration.ofSeconds(2);

    /** Hard ceiling for the narrow class of synchronous calls that ship a
     *  binary payload (image upload to Claude vision, S3 PUT). Generous enough
     *  for a few-MB JPEG on a slow connection; anything longer should be
     *  fire-and-forget on an event handler, not a blocking call. */
    Duration MAX_UPLOAD_TIMEOUT = Duration.ofSeconds(30);

    /** Hard ceiling for circuit-breaker recovery wait. An hour is already
     *  long enough to bridge most upstream incidents; anything longer is
     *  almost always a typo. Distinct from request timeout — this measures
     *  how long the breaker stays open, not how long any one call may run. */
    Duration MAX_OPEN_WAIT = Duration.ofHours(1);

    String name();

    record RetryConfig(String name, int maxAttempts, Duration backoff)
            implements ResilienceConfig {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .inRange(maxAttempts, 1, 10, "maxAttempts")
                    .notNull(backoff, "backoff")
                    .inRange(backoff, Duration.ZERO, Duration.ofSeconds(1), "backoff");
        }
    }

    /**
     * Synchronous request-path timeout. Defaults to the 2-second
     * {@link #MAX_TIMEOUT} ceiling. Calls that legitimately need longer (image
     * upload, S3 PUT) declare {@code upload = true} and accept the wider
     * {@link #MAX_UPLOAD_TIMEOUT} ceiling — that flag is the place reviewers
     * push back: anything else needing 30 seconds belongs on an event handler.
     */
    record TimeoutConfig(String name, Duration duration, boolean upload)
            implements ResilienceConfig {

        public TimeoutConfig(String name, Duration duration) {
            this(name, duration, false);
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            Duration ceiling = upload ? MAX_UPLOAD_TIMEOUT : MAX_TIMEOUT;
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .notNull(duration, "duration")
                    .inRange(duration, Duration.ofMillis(1), ceiling, "duration");
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
                    .inRange(failureRateThreshold, 1.0f, 100.0f, "failureRateThreshold")
                    .notNull(slowCallDurationThreshold, "slowCallDurationThreshold")
                    .inRange(slowCallDurationThreshold, Duration.ofMillis(1), MAX_UPLOAD_TIMEOUT, "slowCallDurationThreshold")
                    .inRange(slowCallRateThreshold, 1.0f, 100.0f, "slowCallRateThreshold")
                    .notNull(waitDurationInOpenState, "waitDurationInOpenState")
                    .inRange(waitDurationInOpenState, Duration.ofMillis(1), MAX_OPEN_WAIT, "waitDurationInOpenState")
                    .inRange(minimumNumberOfCalls, 1, 10_000, "minimumNumberOfCalls");
        }
    }

    record BulkheadConfig(String name, int maxConcurrentCalls, Duration maxWaitDuration)
            implements ResilienceConfig {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return (Constraints c) -> c
                    .notBlank(name, "name")
                    .inRange(maxConcurrentCalls, 1, 10_000, "maxConcurrentCalls")
                    .notNull(maxWaitDuration, "maxWaitDuration")
                    .inRange(maxWaitDuration, Duration.ZERO, Duration.ofMinutes(1), "maxWaitDuration");
        }
    }
}
