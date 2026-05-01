package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.RetryConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Worked example: a synthetic flaky supplier wrapped with retry + timeout +
 * circuit breaker via the kernel facade. Demonstrates the composition shape
 * domain code will use for cross-boundary calls.
 */
class Resilience4jCompositionTest {

    private static final String STRATEGY = "catalog.fanout";

    @Test
    void retryThenTimeoutThenCircuitBreaker_recoverFromTransientFailure() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RetryConfig(STRATEGY, 4, Duration.ofMillis(1)),
                new TimeoutConfig(STRATEGY, Duration.ofSeconds(1)),
                new CircuitBreakerConfig(STRATEGY,
                        50.0f,
                        Duration.ofSeconds(10),
                        100.0f,
                        Duration.ofSeconds(60),
                        10)));

        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> work = () -> {
            int n = attempts.incrementAndGet();
            if (n < 3) {
                throw new IllegalStateException("transient #" + n);
            }
            return "resolved";
        };

        String result = resilience.circuitBreaker(STRATEGY).execute(() ->
                resilience.timeout(STRATEGY).execute(() ->
                        resilience.retry(STRATEGY).execute(work)));

        assertThat(result).isEqualTo("resolved");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void breaker_tripsAfterPersistentFailures() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RetryConfig(STRATEGY, 1, Duration.ofMillis(1)),
                new CircuitBreakerConfig(STRATEGY,
                        50.0f,
                        Duration.ofSeconds(10),
                        100.0f,
                        Duration.ofSeconds(60),
                        4)));

        Supplier<String> alwaysFails = () -> {
            throw new IllegalStateException("permanent");
        };

        for (int i = 0; i < 4; i++) {
            try {
                resilience.circuitBreaker(STRATEGY)
                        .execute(() -> resilience.retry(STRATEGY).execute(alwaysFails));
            } catch (IllegalStateException ignored) {
                // counted by the breaker
            }
        }

        assertThatThrownBy(() -> resilience.circuitBreaker(STRATEGY).execute(() -> "blocked"))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
