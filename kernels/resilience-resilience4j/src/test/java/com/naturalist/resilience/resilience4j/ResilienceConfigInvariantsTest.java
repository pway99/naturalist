package com.naturalist.resilience.resilience4j;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.resilience.ResilienceConfig.BulkheadConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.RetryConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceConfigInvariantsTest {

    @Test
    void adapter_rejectsNullConfigList() {
        assertThatThrownBy(() -> new Resilience4jResilience(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("configs");
    }

    @Test
    void adapter_rejectsRetryConfigWithBlankName() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RetryConfig(" ", 3, Duration.ofMillis(1)))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void adapter_rejectsRetryConfigWithNullBackoff() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RetryConfig("fine", 3, null))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("backoff");
    }

    @Test
    void adapter_rejectsTimeoutConfigWithNullDuration() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new TimeoutConfig("slow", null))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("duration");
    }

    @Test
    void adapter_rejectsCircuitBreakerConfigWithNullSlowCallDuration() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new CircuitBreakerConfig("trippy",
                        50.0f, null, 100.0f, Duration.ofSeconds(60), 4))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("slowCallDurationThreshold");
    }

    @Test
    void adapter_rejectsBulkheadConfigWithNullMaxWait() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new BulkheadConfig("narrow", 1, null))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("maxWaitDuration");
    }

    @Test
    void adapter_reportsAllViolationsInOnePass() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RetryConfig(" ", 3, null),
                new TimeoutConfig("", null))))
                .isInstanceOf(InvariantViolationException.class)
                .satisfies(e -> {
                    String message = e.getMessage();
                    assertThat(message).contains("name");
                    assertThat(message).contains("backoff");
                    assertThat(message).contains("duration");
                });
    }

    @Test
    void adapter_acceptsValidConfigBundle() {
        Resilience4jResilience adapter = new Resilience4jResilience(List.of(
                new RetryConfig("ok", 3, Duration.ofMillis(1)),
                new TimeoutConfig("ok", Duration.ofMillis(100)),
                new BulkheadConfig("ok", 1, Duration.ZERO)));

        assertThat(adapter.retry("ok")).isNotNull();
        assertThat(adapter.timeout("ok")).isNotNull();
        assertThat(adapter.bulkhead("ok")).isNotNull();
    }
}
