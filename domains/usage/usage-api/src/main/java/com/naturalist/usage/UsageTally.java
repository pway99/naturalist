package com.naturalist.usage;

import com.naturalist.ddd.Entity;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A running identification-usage count for one {@link UsageCounter} over one
 * period slug ({@code daily-<yyyy-mm-dd>}, {@code monthly-<yyyy-mm>}, or
 * {@code rate-<yyyy-mm-dd-hh-mm>}). {@code naturalist} is null for a global
 * (non-per-user) tally.
 */
public record UsageTally(
        UsageTallyId id,
        UsageCounterName counter,
        @Nullable NaturalistName naturalist,
        String period,
        int count
) implements Entity<UsageTallyId> {

    public UsageTally withCount(int newCount) {
        return new UsageTally(id, counter, naturalist, period, newCount);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(counter, "counter")
                .notBlank(period, "period")
                .whenNotNull(naturalist, c -> c.identifier(naturalist, "naturalist"));
    }
}
