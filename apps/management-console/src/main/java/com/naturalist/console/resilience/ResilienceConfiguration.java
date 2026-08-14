package com.naturalist.console.resilience;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Composition root for the resilience strategies referenced by
 * {@code @Resilient(name = ...)} call sites in the assembled application.
 *
 * <h2>Registered strategies</h2>
 * <ul>
 *   <li><b>{@code catalog.fanout}</b> — bounds each
 *       {@code Catalog.findReferencesTo} provider invocation: a 200&nbsp;ms
 *       timeout (a fan-out is on the request path; anything slower belongs on
 *       a worker) plus a circuit breaker so a consistently failing provider
 *       stops being called for thirty seconds before the next probe.</li>
 *   <li><b>{@code image.conversion}</b> — bounds the {@code sips} HEIC→JPEG
 *       subprocess in {@code InsectsController.image}: a 2&nbsp;s timeout (the
 *       full request-path ceiling — image conversion is the lone serving call
 *       at this size, so it is allowed the headroom).</li>
 *   <li><b>{@code vision.identification}</b> — bounds the Anthropic vision API
 *       call in {@code AnthropicVisionService}. Declared {@code upload = true}
 *       and given the full {@link ResilienceConfig#MAX_UPLOAD_TIMEOUT}: the
 *       request ships base64 image bytes, which is precisely the narrow class
 *       that ceiling exists for.</li>
 * </ul>
 *
 * <h2>Status — the marker is not load-bearing</h2>
 * There is no AOP weaver. {@code @Resilient} triggers no interception; a call
 * is protected only where a class takes a {@link Resilience} constructor
 * parameter and wraps the call through the facade itself. The annotation
 * declares <em>where the boundary is</em>, which the call site then honours —
 * a split that lets {@code InMemoryCatalog} apply its strategy per-provider
 * inside the fan-out loop rather than around the whole method, and keeps
 * request-path calls off the timeout adapter's worker pool except where that
 * hop is intended.
 *
 * <p>Two gates keep the declaration and the code honest, replacing the weaver
 * that would otherwise be needed to make the marker mean something:
 * {@link ResilienceNameValidator} fails boot when a declared name resolves to
 * no config, and {@code ResilienceComplianceTest} fails the build when a
 * declaring class never reaches the facade at all.
 *
 * <h2>Diagnostics surface</h2>
 * The set of registered strategy names is published through the secure
 * {@code /admin/resilience} console page rather than at-startup logs — a
 * deliberate choice to keep operational state behind authentication and avoid
 * pushing diagnostic data into a log aggregator.
 */
@Configuration
public class ResilienceConfiguration {

    @Bean
    TimeoutConfig catalogFanoutTimeout() {
        return new TimeoutConfig("catalog.fanout", Duration.ofMillis(200));
    }

    @Bean
    CircuitBreakerConfig catalogFanoutCircuitBreaker() {
        return new CircuitBreakerConfig(
                "catalog.fanout",
                50.0f,
                Duration.ofMillis(200),
                80.0f,
                Duration.ofSeconds(30),
                5);
    }

    @Bean
    TimeoutConfig imageConversionTimeout() {
        return new TimeoutConfig("image.conversion", Duration.ofSeconds(2));
    }

    @Bean
    TimeoutConfig visionIdentificationTimeout() {
        return new TimeoutConfig("vision.identification", ResilienceConfig.MAX_UPLOAD_TIMEOUT, true);
    }

    @Bean
    Resilience resilience(List<ResilienceConfig> configs) {
        return new Resilience4jResilience(configs);
    }

    @Bean
    ApplicationRunner resilienceNameValidator(ApplicationContext context, Resilience resilience) {
        return new ResilienceNameValidator(context, resilience);
    }
}
