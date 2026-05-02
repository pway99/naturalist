package com.naturalist.console.resilience;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
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
 * </ul>
 *
 * <h2>Status — annotation-only today</h2>
 * The configs are registered and the {@link Resilience} bean is built, so any
 * call site that takes a {@link Resilience} constructor parameter and uses the
 * facade programmatically gets the configured behaviour. The
 * {@code @Resilient} marker itself does not yet trigger interception — there
 * is no AOP weaver wired. The annotations are the durable declaration of
 * intent that the M9 ArchUnit gate (and any future weaver) reads; runtime
 * application of the marker is the follow-up tracked in the plan's M8 status.
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
    Resilience resilience(List<ResilienceConfig> configs) {
        return new Resilience4jResilience(configs);
    }
}
