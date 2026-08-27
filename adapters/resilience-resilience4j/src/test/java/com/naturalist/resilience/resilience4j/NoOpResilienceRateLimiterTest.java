package com.naturalist.resilience.resilience4j;

import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Placeholder coverage for the framework {@code RateLimiter} primitive's
 * no-op behaviour. {@code kernels/framework} carries no {@code src/test} of
 * its own — it sits at the root of the module DAG and cannot depend on
 * {@code framework-test} (every other kernel gets JUnit/AssertJ transitively
 * through that dependency; framework-test depends on framework, not the
 * reverse). Per the kernel testing convention
 * ({@code kernels/CLAUDE.md} — "No immediate consumer"), this adapter module
 * — which already depends on framework and carries its own JUnit/AssertJ —
 * hosts the placeholder test until a real consumer (Tasks 16/17, the vision
 * rate limit) exercises {@code Resilience.noOp().rateLimiter(...)} directly.
 */
class NoOpResilienceRateLimiterTest {

    @Test
    void rateLimiter_runsSupplierUnprotected() {
        RateLimiter rateLimiter = Resilience.noOp().rateLimiter("x");

        assertThat(rateLimiter.execute(() -> 42)).isEqualTo(42);
    }

    @Test
    void rateLimiter_runsRunnableUnprotected() {
        RateLimiter rateLimiter = Resilience.noOp().rateLimiter("x");
        boolean[] ran = {false};

        rateLimiter.execute(() -> ran[0] = true);

        assertThat(ran[0]).isTrue();
    }

    @Test
    void rateLimiterNames_empty() {
        assertThat(Resilience.noOp().rateLimiterNames()).isEmpty();
    }
}
