package com.naturalist.resilience.resilience4j;

import com.naturalist.exception.UnconfiguredResilienceException;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.*;
import com.naturalist.resilience.ResilienceConfig.BulkheadConfig;
import com.naturalist.resilience.ResilienceConfig.CircuitBreakerConfig;
import com.naturalist.resilience.ResilienceConfig.RetryConfig;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Bridges {@link Resilience} to Resilience4j. Constructed from a list of
 * {@link ResilienceConfig} records — one composition-root call walks every
 * config's invariants via the framework {@link Observer} and the resulting
 * adapter is immutable post-construction.
 *
 * <p>Lookup by an unknown name throws
 * {@link UnconfiguredResilienceException}. An unconfigured strategy is a
 * deployment defect — the composition root never registered configs for a
 * name domain code referenced — and the adapter refuses to serve an
 * unprotected fall-back. Resilience4j's own metrics cover the configured paths.
 */
public final class Resilience4jResilience implements Resilience {

    private final Map<String, Retry> retries;
    private final Map<String, Timeout> timeouts;
    private final Map<String, CircuitBreaker> breakers;
    private final Map<String, Bulkhead> bulkheads;

    public Resilience4jResilience(List<ResilienceConfig> configs) {
        Observer observer = Observer.forClass(Resilience4jResilience.class);
        observer.arguments("construct", c -> c.notNull(configs, "configs"))
                .throwWhenInvalid();
        observer.arguments("construct", c -> {
            for (int i = 0; i < configs.size(); i++) {
                c.observable(configs.get(i), Function.identity(), "configs[" + i + "]");
            }
        }).throwWhenInvalid();

        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        CircuitBreakerRegistry breakerRegistry = CircuitBreakerRegistry.ofDefaults();
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();

        Map<String, Retry> retryMap = new HashMap<>();
        Map<String, Timeout> timeoutMap = new HashMap<>();
        Map<String, CircuitBreaker> breakerMap = new HashMap<>();
        Map<String, Bulkhead> bulkheadMap = new HashMap<>();

        for (ResilienceConfig config : configs) {
            switch (config) {
                case RetryConfig c -> retryMap.put(
                        c.name(),
                        new Resilience4jRetry(retryRegistry.retry(c.name(), toR4j(c))));
                case TimeoutConfig c -> timeoutMap.put(
                        c.name(),
                        new Resilience4jTimeout(timeLimiterRegistry.timeLimiter(c.name(), toR4j(c))));
                case CircuitBreakerConfig c -> breakerMap.put(
                        c.name(),
                        new Resilience4jCircuitBreaker(breakerRegistry.circuitBreaker(c.name(), toR4j(c))));
                case BulkheadConfig c -> bulkheadMap.put(
                        c.name(),
                        new Resilience4jBulkhead(bulkheadRegistry.bulkhead(c.name(), toR4j(c))));
            }
        }

        this.retries = Map.copyOf(retryMap);
        this.timeouts = Map.copyOf(timeoutMap);
        this.breakers = Map.copyOf(breakerMap);
        this.bulkheads = Map.copyOf(bulkheadMap);
    }

    @Override
    public Retry retry(String name) {
        Retry r = retries.get(name);
        if (r == null) throw new UnconfiguredResilienceException("retry", name);
        return r;
    }

    @Override
    public Timeout timeout(String name) {
        Timeout t = timeouts.get(name);
        if (t == null) throw new UnconfiguredResilienceException("timeout", name);
        return t;
    }

    @Override
    public CircuitBreaker circuitBreaker(String name) {
        CircuitBreaker b = breakers.get(name);
        if (b == null) throw new UnconfiguredResilienceException("circuit-breaker", name);
        return b;
    }

    @Override
    public Bulkhead bulkhead(String name) {
        Bulkhead b = bulkheads.get(name);
        if (b == null) throw new UnconfiguredResilienceException("bulkhead", name);
        return b;
    }

    @Override
    public Set<String> retryNames() {
        return retries.keySet();
    }

    @Override
    public Set<String> timeoutNames() {
        return timeouts.keySet();
    }

    @Override
    public Set<String> circuitBreakerNames() {
        return breakers.keySet();
    }

    @Override
    public Set<String> bulkheadNames() {
        return bulkheads.keySet();
    }

    static io.github.resilience4j.retry.RetryConfig toR4j(RetryConfig c) {
        return io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(c.maxAttempts())
                .waitDuration(c.backoff())
                .build();
    }

    static TimeLimiterConfig toR4j(TimeoutConfig c) {
        return TimeLimiterConfig.custom()
                .timeoutDuration(c.duration())
                .cancelRunningFuture(true)
                .build();
    }

    static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig toR4j(CircuitBreakerConfig c) {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(c.failureRateThreshold())
                .slowCallDurationThreshold(c.slowCallDurationThreshold())
                .slowCallRateThreshold(c.slowCallRateThreshold())
                .waitDurationInOpenState(c.waitDurationInOpenState())
                .minimumNumberOfCalls(c.minimumNumberOfCalls())
                .build();
    }

    static io.github.resilience4j.bulkhead.BulkheadConfig toR4j(BulkheadConfig c) {
        return io.github.resilience4j.bulkhead.BulkheadConfig.custom()
                .maxConcurrentCalls(c.maxConcurrentCalls())
                .maxWaitDuration(c.maxWaitDuration())
                .build();
    }
}
