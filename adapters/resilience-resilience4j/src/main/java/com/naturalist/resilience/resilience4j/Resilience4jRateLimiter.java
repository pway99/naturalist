package com.naturalist.resilience.resilience4j;

import com.naturalist.observability.Observer;
import com.naturalist.resilience.RateLimitExceededException;
import com.naturalist.resilience.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One strategy's limiter. The unkeyed instance wraps the registry's limiter for the
 * strategy name; {@link #forKey(String)} hands out one further instance per key, built
 * from the same {@link RateLimiterConfig} and held in a bounded least-recently-used map.
 *
 * <p>The bound ({@link #MAX_KEYS}) is what keeps a key-rotating caller from growing the
 * adapter's memory without limit: past it the least-recently-used key is dropped and its
 * budget starts afresh. That is acceptable because a per-key limiter is only ever the
 * targeted half of a pair — the system-wide ceiling on the unkeyed limiter is what bounds
 * a caller who rotates keys. The key never reaches the vendor rejection's strategy name
 * or the kernel exception: both carry the strategy, so a key is never logged by accident.
 */
final class Resilience4jRateLimiter implements RateLimiter {

    /** Distinct live keys per strategy before the least-recently-used one is evicted. */
    static final int MAX_KEYS = 10_000;

    private static final Observer observer = Observer.forClass(Resilience4jRateLimiter.class);

    private final String strategy;
    private final io.github.resilience4j.ratelimiter.RateLimiter delegate;
    private final RateLimiterConfig config;
    /** Access-ordered; {@code null} on a keyed instance, whose {@code forKey} returns itself. */
    private final @Nullable Map<String, RateLimiter> keyed;

    Resilience4jRateLimiter(io.github.resilience4j.ratelimiter.RateLimiter delegate, RateLimiterConfig config) {
        this(delegate.getName(), delegate, config, new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RateLimiter> eldest) {
                return size() > MAX_KEYS;
            }
        });
    }

    private Resilience4jRateLimiter(String strategy,
                                    io.github.resilience4j.ratelimiter.RateLimiter delegate,
                                    RateLimiterConfig config,
                                    @Nullable Map<String, RateLimiter> keyed) {
        this.strategy = strategy;
        this.delegate = delegate;
        this.config = config;
        this.keyed = keyed;
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        try {
            return delegate.executeSupplier(supplier);
        } catch (RequestNotPermitted e) {
            throw rejected();
        }
    }

    @Override
    public void execute(Runnable runnable) {
        try {
            delegate.executeRunnable(runnable);
        } catch (RequestNotPermitted e) {
            throw rejected();
        }
    }

    @Override
    public RateLimiter forKey(String key) {
        observer.arguments("forKey", i -> i.notBlank(key, "key")).throwWhenInvalid();
        if (keyed == null) {
            return this;
        }
        synchronized (keyed) {
            return keyed.computeIfAbsent(key, k -> new Resilience4jRateLimiter(
                    strategy,
                    io.github.resilience4j.ratelimiter.RateLimiter.of(strategy + ":" + k, config),
                    config,
                    null));
        }
    }

    /**
     * Translates the vendor rejection into the kernel exception — the vendor
     * type {@link RequestNotPermitted} must never escape this adapter. Names the
     * strategy, never the key.
     */
    private RateLimitExceededException rejected() {
        return new RateLimitExceededException(
                "Rate limit exceeded for strategy '" + strategy + "'", strategy);
    }
}
