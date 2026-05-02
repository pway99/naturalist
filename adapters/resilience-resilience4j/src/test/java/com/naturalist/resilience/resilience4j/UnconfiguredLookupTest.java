package com.naturalist.resilience.resilience4j;

import com.naturalist.exception.UnconfiguredResilienceException;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig.RetryConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adapter rejects unconfigured-name lookups with
 * {@link UnconfiguredResilienceException}. An unconfigured strategy is a
 * deployment defect, surfaced loudly at the lookup site rather than absorbed
 * silently by an unprotected fall-back.
 */
class UnconfiguredLookupTest {

    @Test
    void retry_unknownName_throws() {
        Resilience adapter = new Resilience4jResilience(List.of());

        assertThatThrownBy(() -> adapter.retry("missing"))
                .isInstanceOf(UnconfiguredResilienceException.class)
                .satisfies(e -> {
                    UnconfiguredResilienceException u = (UnconfiguredResilienceException) e;
                    assertThat(u.primitive()).isEqualTo("retry");
                    assertThat(u.name()).isEqualTo("missing");
                });
    }

    @Test
    void timeout_unknownName_throws() {
        Resilience adapter = new Resilience4jResilience(List.of());

        assertThatThrownBy(() -> adapter.timeout("missing"))
                .isInstanceOf(UnconfiguredResilienceException.class)
                .hasMessageContaining("timeout")
                .hasMessageContaining("missing");
    }

    @Test
    void circuitBreaker_unknownName_throws() {
        Resilience adapter = new Resilience4jResilience(List.of());

        assertThatThrownBy(() -> adapter.circuitBreaker("missing"))
                .isInstanceOf(UnconfiguredResilienceException.class)
                .hasMessageContaining("circuit-breaker");
    }

    @Test
    void bulkhead_unknownName_throws() {
        Resilience adapter = new Resilience4jResilience(List.of());

        assertThatThrownBy(() -> adapter.bulkhead("missing"))
                .isInstanceOf(UnconfiguredResilienceException.class)
                .hasMessageContaining("bulkhead");
    }

    @Test
    void wrongPrimitiveOnConfiguredName_throws() {
        Resilience adapter = new Resilience4jResilience(List.of(
                new RetryConfig("partial", 1, Duration.ofMillis(1))));

        // Retry is configured under "partial"; timeout is not.
        assertThat(adapter.retry("partial")).isNotNull();
        assertThatThrownBy(() -> adapter.timeout("partial"))
                .isInstanceOf(UnconfiguredResilienceException.class);
    }
}
