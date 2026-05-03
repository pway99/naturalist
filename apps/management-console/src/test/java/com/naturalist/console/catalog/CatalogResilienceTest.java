package com.naturalist.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Synthetic-failure integration test proving the {@code catalog.fanout}
 * resilience strategy actually fires inside {@link Catalog#findReferencesTo}.
 * Mirrors the production wiring — {@link Resilience4jResilience} backing the
 * {@link Catalog} via {@link CatalogAssembly} — without booting Spring, since
 * the failure modes under test are about resilience behaviour rather than
 * Spring composition (covered by {@link CatalogConfigurationTest}).
 *
 * <p>The two scenarios cover the strategy's two halves:
 * <ul>
 *   <li>A provider that hangs longer than the configured timeout has its
 *       call cancelled; the swallow at {@code invokeQuietly} drops the
 *       resulting {@code TimeoutException} and the affected domain
 *       contributes nothing to the response.</li>
 *   <li>A provider that consistently throws trips the circuit breaker after
 *       the configured minimum number of calls; subsequent fan-out requests
 *       short-circuit without invoking the provider — visible as a fixed
 *       call counter on the synthetic provider.</li>
 * </ul>
 */
class CatalogResilienceTest {

    private static final String STRATEGY = "catalog.fanout";

    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "resilience-test";
        }
    }

    @Test
    void slowProviderIsTimedOutAndYieldsEmptyResult() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new TimeoutConfig(STRATEGY, Duration.ofMillis(50)),
                breakerLargeWindow()));

        AtomicInteger calls = new AtomicInteger();
        Catalog catalog = CatalogAssembly.from(
                List.of(),
                List.of(),
                List.of(sleepingProvider(calls, Duration.ofSeconds(2))),
                resilience);

        Map<DomainId, List<EntityRef>> result =
                catalog.findReferencesTo(CompoundName.of("any"));

        assertThat(result).isEmpty();
        assertThat(calls).hasValueGreaterThanOrEqualTo(1);
    }

    @Test
    void breakerOpensAfterPersistentFailuresAndShortCircuitsSubsequentCalls() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new TimeoutConfig(STRATEGY, Duration.ofSeconds(1)),
                new CircuitBreakerConfig(STRATEGY,
                        50.0f,
                        Duration.ofSeconds(10),
                        100.0f,
                        Duration.ofSeconds(60),
                        4)));

        AtomicInteger calls = new AtomicInteger();
        Catalog catalog = CatalogAssembly.from(
                List.of(),
                List.of(),
                List.of(throwingProvider(calls)),
                resilience);

        for (int i = 0; i < 4; i++) {
            catalog.findReferencesTo(CompoundName.of("any"));
        }
        int callsBeforeOpen = calls.get();

        for (int i = 0; i < 5; i++) {
            catalog.findReferencesTo(CompoundName.of("any"));
        }
        int callsAfterOpen = calls.get();

        assertThat(callsBeforeOpen).isGreaterThanOrEqualTo(4);
        assertThat(callsAfterOpen).isEqualTo(callsBeforeOpen);
    }

    private static CircuitBreakerConfig breakerLargeWindow() {
        return new CircuitBreakerConfig(STRATEGY,
                50.0f,
                Duration.ofSeconds(10),
                100.0f,
                Duration.ofSeconds(60),
                10_000);
    }

    private static EntityReferences<CompoundName> sleepingProvider(AtomicInteger calls, Duration sleep) {
        return new EntityReferences<>() {
            @Override
            public DomainId domain() {
                return new TestDomain();
            }

            @Override
            public Class<CompoundName> referenceType() {
                return CompoundName.class;
            }

            @Override
            public Stream<EntityRef> referencesTo(CompoundName target) {
                calls.incrementAndGet();
                try {
                    Thread.sleep(sleep.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Stream.empty();
            }
        };
    }

    private static EntityReferences<CompoundName> throwingProvider(AtomicInteger calls) {
        return new EntityReferences<>() {
            @Override
            public DomainId domain() {
                return new TestDomain();
            }

            @Override
            public Class<CompoundName> referenceType() {
                return CompoundName.class;
            }

            @Override
            public Stream<EntityRef> referencesTo(CompoundName target) {
                calls.incrementAndGet();
                throw new RuntimeException("simulated provider failure");
            }
        };
    }
}
