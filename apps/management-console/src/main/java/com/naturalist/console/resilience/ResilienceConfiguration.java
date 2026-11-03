package com.naturalist.console.resilience;

import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
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
 *   <li><b>{@code image.store}</b> — bounds every {@code FilesystemImageStore}
 *       operation (declared by {@code FilesystemImageStore.STRATEGY}): a 2&nbsp;s
 *       timeout, the full request-path ceiling, since serving a stored photo is
 *       a request-path call.</li>
 *   <li><b>{@code vision.identification}</b> — bounds the Anthropic vision API
 *       call in {@code AnthropicVisionService}. Declared {@code upload = true}
 *       and given the full {@link ResilienceConfig#MAX_UPLOAD_TIMEOUT}: the
 *       request ships base64 image bytes, which is precisely the narrow class
 *       that ceiling exists for. Also carries a {@code RateLimiterConfig}
 *       capping the strategy at three requests per minute with a zero
 *       timeout. The permit is acquired once per identification by
 *       {@code InsectIdentificationCommandImpl} — before the vision call, not
 *       inside {@code AnthropicVisionService}, and not once per vision turn
 *       — so "3/min" genuinely means three identifications per minute. A
 *       caller over the limit is rejected immediately with the kernel
 *       {@code RateLimitExceededException}, never the vendor
 *       {@code RequestNotPermitted}, rather than queued. A {@code BulkheadConfig}
 *       then allows two identifications in flight at once with zero wait, taken
 *       in the same core class right after the rate gate; a third concurrent
 *       call is refused with the kernel {@code BulkheadFullException} (surfaced
 *       as {@code IdentificationRefused.Reason.BUSY}) before any budget is
 *       reserved.</li>
 *   <li><b>{@code vision.identification.client}</b> — the per-client ceiling
 *       {@code IdentifyRateLimitFilter} takes on {@code POST /<domain>/identify}, after
 *       authorization: two a minute per client (keyed by the remote address through
 *       {@code RateLimiter.forKey}), shared across the organism domains and deliberately
 *       below the global three, so one address can never hold every shared slot.
 *       Rejections are {@code 429}, observed as {@code withinClientRate}.</li>
 *   <li><b>{@code auth.login}</b> / <b>{@code auth.login.client}</b> — the pair of
 *       ceilings {@code LoginRateLimitFilter} takes on {@code POST /login} ahead of the
 *       password check: ten attempts a minute per client (keyed by the remote address
 *       through {@code RateLimiter.forKey}), then 300 a minute system-wide, so one abuser
 *       is cut off on their own and a key-rotating run still cannot burn a BCrypt round
 *       per guess without bound. Rejections are {@code 429}, observed as
 *       {@code withinClientRate} / {@code withinRate}; the targeted response to one
 *       persistent abuser remains {@code AccountCommand.suspend}.</li>
 *   <li><b>{@code auth.register}</b> / <b>{@code auth.register.client}</b> — the same
 *       pair for sign-ups, taken by {@code RegistrationTransaction} before any validation
 *       or encoding: three a minute per client, ten a minute system-wide. Every
 *       registration emails an address nobody has proven they own, so an unbounded
 *       endpoint is a mail-bombing primitive.</li>
 * </ul>
 *
 * <p>What "client" means is a deployment decision, not a header the app reads:
 * {@code HttpServletRequest.getRemoteAddr()} is the TCP peer unless Spring Boot's
 * {@code server.forward-headers-strategy} is set for a trusted proxy. See
 * {@code apps/CLAUDE.md}, "Client identity behind a proxy".
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
    TimeoutConfig visionIdentificationTimeout() {
        return new TimeoutConfig("vision.identification", ResilienceConfig.MAX_UPLOAD_TIMEOUT, true);
    }

    @Bean
    RateLimiterConfig visionRateLimit() {
        return new RateLimiterConfig("vision.identification", 3, Duration.ofMinutes(1), Duration.ZERO);
    }

    @Bean
    Resilience resilience(List<ResilienceConfig> configs) {
        return new Resilience4jResilience(configs);
    }

    @Bean
    ApplicationRunner resilienceNameValidator(ApplicationContext context, Resilience resilience) {
        return new ResilienceNameValidator(context, resilience);
    }

    @Bean
    ResilienceConfig.RateLimiterConfig authLoginRateLimit() {
        // Global ceiling on authentication attempts: caps the BCrypt CPU a credential-stuffing run
        // can burn and bounds the badCredentials metric's worst case. Sized so real users never
        // see it; the targeted response to one abuser is AccountCommand.suspend, not this.
        return new RateLimiterConfig("auth.login", 300, Duration.ofMinutes(1), Duration.ZERO);
    }

    @Bean
    ResilienceConfig.RateLimiterConfig authRegisterRateLimit() {
        // Global ceiling on sign-ups: each registration sends an email to an address nobody has
        // proven they own, so an unbounded /register is a mail-bombing primitive against third
        // parties. Ten a minute is far above any honest rate for this audience.
        return new RateLimiterConfig("auth.register", 10, Duration.ofMinutes(1), Duration.ZERO);
    }

    @Bean
    ResilienceConfig.RateLimiterConfig authLoginClientRateLimit() {
        // Per-client ceiling on authentication attempts (LoginRateLimitFilter keys it by remote
        // address): ten guesses a minute is generous for a person who mistyped and useless for
        // a dictionary. The global auth.login ceiling above still bounds a key-rotating attacker.
        return new RateLimiterConfig("auth.login.client", 10, Duration.ofMinutes(1), Duration.ZERO);
    }

    @Bean
    ResilienceConfig.RateLimiterConfig authRegisterClientRateLimit() {
        // Per-client ceiling on sign-ups (RegistrationTransaction keys it by the client key the
        // controller derives from the remote address): three a minute from one address covers
        // a household or a classroom; more is a script. The global auth.register ceiling stays.
        return new RateLimiterConfig("auth.register.client", 3, Duration.ofMinutes(1), Duration.ZERO);
    }

    @Bean
    TimeoutConfig imageStoreTimeout() {
        return new TimeoutConfig(FilesystemImageStore.STRATEGY, Duration.ofSeconds(2));
    }

    @Bean
    ResilienceConfig.BulkheadConfig visionBulkhead() {
        // Two identifications in flight at once, zero wait: one more than a single honest use, and
        // a ceiling on how many vision calls a burst can hold open against the model credential.
        return new ResilienceConfig.BulkheadConfig("vision.identification", 2, Duration.ZERO);
    }

    @Bean
    ResilienceConfig.RateLimiterConfig visionClientRateLimit() {
        // Per-client ceiling on identification (IdentifyRateLimitFilter keys it by remote address):
        // two a minute, below the global three, so one address can never hold every shared slot.
        return new RateLimiterConfig("vision.identification.client", 2, Duration.ofMinutes(1), Duration.ZERO);
    }
}
