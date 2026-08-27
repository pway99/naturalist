package com.naturalist.usage;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * An editable budget rule metering identification usage for one
 * {@link UsageCounterName} activity — how it is scoped ({@link UsageScope}),
 * what window it measures ({@link WindowKind}), and the {@code limit} that
 * window enforces. Multiple rules may target the same {@code counterName}
 * (e.g. a per-user daily rule alongside a global monthly rule); the pair
 * {@code (counterName, scope, windowKind)} is unique.
 */
public record UsageCounter(UsageCounterId id, UsageCounterName counterName, UsageScope scope,
                           WindowKind windowKind, @Nullable Instant since, int limit,
                           boolean active) implements Entity<UsageCounterId> {

    public UsageCounter withLimit(int newLimit) {
        return new UsageCounter(id, counterName, scope, windowKind, since, newLimit, active);
    }

    public UsageCounter withActive(boolean newActive) {
        return new UsageCounter(id, counterName, scope, windowKind, since, limit, newActive);
    }

    public UsageCounter withSince(Instant newSince) {
        return new UsageCounter(id, counterName, scope, windowKind, newSince, limit, active);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(counterName, "counterName")
                .notNull(scope, "scope")
                .notNull(windowKind, "windowKind")
                .atLeast(limit, 0, "limit")
                .isTrue(windowKind != WindowKind.SINCE || since != null, "since");
    }
}
