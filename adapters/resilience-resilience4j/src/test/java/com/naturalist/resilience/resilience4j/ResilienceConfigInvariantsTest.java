package com.naturalist.resilience.resilience4j;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.resilience.ResilienceConfig.BulkheadConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
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
    void adapter_rejectsRetryConfigWithZeroMaxAttempts() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RetryConfig("fine", 0, Duration.ofMillis(1)))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("maxAttempts");
    }

    @Test
    void adapter_rejectsRetryConfigWithExcessiveMaxAttempts() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RetryConfig("fine", 100, Duration.ofMillis(1)))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("maxAttempts");
    }

    @Test
    void adapter_rejectsRetryConfigWithExcessiveBackoff() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RetryConfig("fine", 3, Duration.ofSeconds(30)))))
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
    void adapter_rejectsTimeoutConfigWithZeroDuration() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new TimeoutConfig("slow", Duration.ZERO))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("duration");
    }

    @Test
    void adapter_rejectsTimeoutConfigWithOneDayDuration() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new TimeoutConfig("slow", Duration.ofDays(1)))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("duration");
    }

    @Test
    void adapter_rejectsRequestPathTimeoutAboveTwoSeconds() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new TimeoutConfig("slow", Duration.ofSeconds(5)))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("duration");
    }

    @Test
    void adapter_acceptsUploadTimeoutAboveRequestPathCeiling() {
        Resilience4jResilience adapter = new Resilience4jResilience(List.of(
                new TimeoutConfig("upload", Duration.ofSeconds(20), true)));

        assertThat(adapter.timeout("upload")).isNotNull();
    }

    @Test
    void adapter_rejectsUploadTimeoutAboveThirtySeconds() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new TimeoutConfig("upload", Duration.ofMinutes(2), true))))
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
    void adapter_rejectsCircuitBreakerConfigWithFailureRateBelowOne() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new CircuitBreakerConfig("trippy",
                        0.0f, Duration.ofSeconds(10), 100.0f, Duration.ofSeconds(60), 4))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("failureRateThreshold");
    }

    @Test
    void adapter_rejectsCircuitBreakerConfigWithFailureRateAboveOneHundred() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new CircuitBreakerConfig("trippy",
                        150.0f, Duration.ofSeconds(10), 100.0f, Duration.ofSeconds(60), 4))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("failureRateThreshold");
    }

    @Test
    void adapter_rejectsCircuitBreakerConfigWithExcessiveOpenWait() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new CircuitBreakerConfig("trippy",
                        50.0f, Duration.ofSeconds(10), 100.0f, Duration.ofDays(1), 4))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("waitDurationInOpenState");
    }

    @Test
    void adapter_rejectsCircuitBreakerConfigWithZeroMinimumCalls() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new CircuitBreakerConfig("trippy",
                        50.0f, Duration.ofSeconds(10), 100.0f, Duration.ofSeconds(60), 0))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("minimumNumberOfCalls");
    }

    @Test
    void adapter_rejectsBulkheadConfigWithNullMaxWait() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new BulkheadConfig("narrow", 1, null))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("maxWaitDuration");
    }

    @Test
    void adapter_rejectsBulkheadConfigWithZeroMaxConcurrent() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new BulkheadConfig("narrow", 0, Duration.ZERO))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("maxConcurrentCalls");
    }

    @Test
    void adapter_rejectsBulkheadConfigWithExcessiveMaxWait() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new BulkheadConfig("narrow", 1, Duration.ofDays(1)))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("maxWaitDuration");
    }

    @Test
    void adapter_rejectsRateLimiterConfigWithZeroLimitForPeriod() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", 0, Duration.ofSeconds(1), Duration.ZERO))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("limitForPeriod");
    }

    @Test
    void adapter_rejectsRateLimiterConfigWithNegativeLimitForPeriod() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", -1, Duration.ofSeconds(1), Duration.ZERO))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("limitForPeriod");
    }

    @Test
    void adapter_rejectsRateLimiterConfigWithNullRefreshPeriod() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", 2, null, Duration.ZERO))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("limitRefreshPeriod");
    }

    @Test
    void adapter_rejectsRateLimiterConfigWithRefreshPeriodAboveOneMinute() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", 2, Duration.ofMinutes(5), Duration.ZERO))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("limitRefreshPeriod");
    }

    @Test
    void adapter_rejectsRateLimiterConfigWithNullTimeoutDuration() {
        assertThatThrownBy(() -> new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", 2, Duration.ofSeconds(1), null))))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("timeoutDuration");
    }

    @Test
    void adapter_acceptsValidRateLimiterConfig() {
        Resilience4jResilience adapter = new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", 2, Duration.ofSeconds(1), Duration.ZERO)));

        assertThat(adapter.rateLimiter("throttled")).isNotNull();
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
