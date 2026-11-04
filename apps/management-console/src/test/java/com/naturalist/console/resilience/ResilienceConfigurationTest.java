package com.naturalist.console.resilience;

import com.naturalist.exception.UnconfiguredResilienceException;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@link Resilience} bean exposes exactly the strategies
 * declared by {@link ResilienceConfiguration} — the names referenced by
 * {@code @Resilient(name = ...)} call sites must resolve here, and names
 * that were never registered must surface as
 * {@link UnconfiguredResilienceException} rather than silently falling
 * back to a no-op.
 */
@SpringBootTest
class ResilienceConfigurationTest {

    @Autowired
    Resilience resilience;

    @Test
    void resolvesCatalogFanoutTimeout() {
        assertThat(resilience.timeout("catalog.fanout")).isNotNull();
    }

    @Test
    void resolvesCatalogFanoutCircuitBreaker() {
        assertThat(resilience.circuitBreaker("catalog.fanout")).isNotNull();
    }

    @Test
    void resolvesVisionIdentificationTimeout() {
        assertThat(resilience.timeout("vision.identification")).isNotNull();
    }

    @Test
    void resolvesVisionIdentificationRateLimiter() {
        assertThat(resilience.rateLimiter("vision.identification")).isNotNull();
        assertThat(resilience.rateLimiterNames()).contains("vision.identification");
    }

    @Test
    void rejectsUnconfiguredName() {
        assertThatThrownBy(() -> resilience.retry("does.not.exist"))
                .isInstanceOf(UnconfiguredResilienceException.class);
    }

    @Test
    void resolvesAuthLoginRateLimiter() {
        assertThat(resilience.rateLimiter("auth.login")).isNotNull();
        assertThat(resilience.rateLimiterNames()).contains("auth.login");
    }

    @Test
    void resolvesAuthRegisterRateLimiter() {
        assertThat(resilience.rateLimiter("auth.register")).isNotNull();
        assertThat(resilience.rateLimiterNames()).contains("auth.register");
    }

    @Test
    void resolvesAuthLoginClientRateLimiter() {
        assertThat(resilience.rateLimiter("auth.login.client")).isNotNull();
        assertThat(resilience.rateLimiterNames()).contains("auth.login.client");
    }

    @Test
    void resolvesAuthRegisterClientRateLimiter() {
        assertThat(resilience.rateLimiter("auth.register.client")).isNotNull();
        assertThat(resilience.rateLimiterNames()).contains("auth.register.client");
    }

    @Test
    void resolvesTheChallengeVerifyStrategy() {
        assertThat(resilience.timeout("challenge.verify")).isNotNull();
        assertThat(resilience.retry("challenge.verify")).isNotNull();
        assertThat(resilience.circuitBreaker("challenge.verify")).isNotNull();
    }

    @Test
    void resolvesImageStoreTimeout() {
        assertThat(resilience.timeout("image.store")).isNotNull();
    }

    @Test
    void resolvesVisionIdentificationBulkhead() {
        assertThat(resilience.bulkhead("vision.identification")).isNotNull();
        assertThat(resilience.bulkheadNames()).containsExactly("vision.identification");
    }

    /** ADR-027 D5: the edge and the challenge are the front line, so these only bound CPU and mail. */
    @Test
    void theGlobalAuthLimits_areBackstopsAtTheAdrValues() {
        ResilienceConfiguration production = new ResilienceConfiguration();

        assertThat(production.authLoginRateLimit().limitForPeriod()).isEqualTo(600);
        assertThat(production.authRegisterRateLimit().limitForPeriod()).isEqualTo(60);
    }
}
