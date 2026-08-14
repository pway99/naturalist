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
    void resolvesImageConversionTimeout() {
        assertThat(resilience.timeout("image.conversion")).isNotNull();
    }

    @Test
    void resolvesVisionIdentificationTimeout() {
        assertThat(resilience.timeout("vision.identification")).isNotNull();
    }

    @Test
    void rejectsUnconfiguredName() {
        assertThatThrownBy(() -> resilience.retry("does.not.exist"))
                .isInstanceOf(UnconfiguredResilienceException.class);
    }
}
