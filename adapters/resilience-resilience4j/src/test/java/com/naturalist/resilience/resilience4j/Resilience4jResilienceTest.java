package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.*;
import com.naturalist.resilience.ResilienceConfig.BulkheadConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
import com.naturalist.resilience.ResilienceConfig.RetryConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Resilience4jResilienceTest {

    @Test
    void retry_executesNAttemptsOnTransientFailure() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RetryConfig("flaky", 3, Duration.ofMillis(1))));

        AtomicInteger attempts = new AtomicInteger();
        String result = resilience.retry("flaky").execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void retry_givesUpAfterMaxAttempts() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RetryConfig("doomed", 2, Duration.ofMillis(1))));

        AtomicInteger attempts = new AtomicInteger();
        Retry retry = resilience.retry("doomed");

        assertThatThrownBy(() -> retry.execute(() -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("permanent");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(attempts).hasValue(2);
    }

    @Test
    void timeout_firesAfterConfiguredDuration() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new TimeoutConfig("slow", Duration.ofMillis(50))));

        Timeout timeout = resilience.timeout("slow");

        assertThatThrownBy(() -> timeout.execute(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "should-not-return";
        })).isInstanceOf(Resilience4jTimeout.ResilienceTimeoutException.class);
    }

    @Test
    void timeout_returnsResultWhenWithinDuration() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new TimeoutConfig("quick", Duration.ofMillis(500))));

        String result = resilience.timeout("quick").execute(() -> "fast");

        assertThat(result).isEqualTo("fast");
    }

    @Test
    void circuitBreaker_opensAfterThreshold() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new CircuitBreakerConfig("trippy",
                        50.0f,
                        Duration.ofSeconds(10),
                        100.0f,
                        Duration.ofSeconds(60),
                        4)));

        CircuitBreaker breaker = resilience.circuitBreaker("trippy");

        for (int i = 0; i < 4; i++) {
            try {
                breaker.execute(() -> {
                    throw new IllegalStateException("fail");
                });
            } catch (IllegalStateException ignored) {
                // expected — counts toward the breaker's failure rate
            }
        }

        assertThatThrownBy(() -> breaker.execute(() -> "after-open"))
                .isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void registeredNames_groupedByPrimitive() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RetryConfig("flaky", 3, Duration.ofMillis(1)),
                new TimeoutConfig("slow", Duration.ofMillis(50)),
                new TimeoutConfig("snappy", Duration.ofMillis(50)),
                new CircuitBreakerConfig("trippy",
                        50.0f,
                        Duration.ofSeconds(10),
                        100.0f,
                        Duration.ofSeconds(60),
                        4),
                new BulkheadConfig("narrow", 1, Duration.ZERO),
                new RateLimiterConfig("throttled", 2, Duration.ofSeconds(1), Duration.ZERO)));

        assertThat(resilience.retryNames()).containsExactly("flaky");
        assertThat(resilience.timeoutNames()).containsExactlyInAnyOrder("slow", "snappy");
        assertThat(resilience.circuitBreakerNames()).containsExactly("trippy");
        assertThat(resilience.bulkheadNames()).containsExactly("narrow");
        assertThat(resilience.rateLimiterNames()).containsExactly("throttled");
    }

    @Test
    void registeredNames_emptyWhenNothingRegistered() {
        Resilience resilience = new Resilience4jResilience(List.of());

        assertThat(resilience.retryNames()).isEmpty();
        assertThat(resilience.timeoutNames()).isEmpty();
        assertThat(resilience.circuitBreakerNames()).isEmpty();
        assertThat(resilience.bulkheadNames()).isEmpty();
        assertThat(resilience.rateLimiterNames()).isEmpty();
    }

    @Test
    void rateLimiter_permitsUpToLimitThenRejects() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RateLimiterConfig("throttled", 2, Duration.ofSeconds(1), Duration.ZERO)));

        RateLimiter rateLimiter = resilience.rateLimiter("throttled");

        assertThat(rateLimiter.execute(() -> "first")).isEqualTo("first");
        assertThat(rateLimiter.execute(() -> "second")).isEqualTo("second");

        assertThatThrownBy(() -> rateLimiter.execute(() -> "third"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void bulkhead_rejectsBeyondCapacity() throws Exception {
        Resilience resilience = new Resilience4jResilience(List.of(
                new BulkheadConfig("narrow", 1, Duration.ZERO)));

        Bulkhead bulkhead = resilience.bulkhead("narrow");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch holdInside = new CountDownLatch(1);
        CountDownLatch insideStarted = new CountDownLatch(1);

        try {
            Future<String> first = pool.submit(() -> bulkhead.execute(() -> {
                insideStarted.countDown();
                try {
                    holdInside.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "first";
            }));

            insideStarted.await();

            Future<String> second = pool.submit(() -> bulkhead.execute(() -> "second"));

            assertThatThrownBy(second::get)
                    .hasCauseInstanceOf(BulkheadFullException.class);

            holdInside.countDown();
            assertThat(first.get()).isEqualTo("first");
        } finally {
            pool.shutdownNow();
        }
    }

}
