package com.naturalist.usage;

import com.naturalist.ddd.ReadModel;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Read port over the current identification-usage picture — the CQS query half of
 * what was a single combined read/write service. Aggregate display state
 * ({@link #snapshot()}), the alert list ({@link #activeAlerts()}), and the batched
 * per-rule usage counts {@link UsageCommand#reserve} needs to evaluate its
 * all-or-nothing check ({@link #reserveState(UsageCounterName, NaturalistName, Instant)}).
 *
 * <p>The adapter ({@code UsageQueryImpl} in {@code usage-core}) is named and shaped
 * like {@code InsectQueryImpl} specifically so it is recognised as a head-of-DAG
 * query by the N+1 no-fan-out select gate
 * (see {@code kernels/framework-test}'s {@code com.naturalist.test.query.nofanout}):
 * every batched repository read this port makes is now gated against fan-out.
 */
public interface UsageQuery {

    UsageSnapshot snapshot();

    /** Unacknowledged alerts, newest first. */
    List<UsageAlert> activeAlerts();

    /**
     * Every active rule for {@code counter}, each paired with the number of
     * {@link UsageEvent}s that count against it as of {@code now} — resolved in
     * ONE batched {@link UsageRepository.EventRepository#findByCounterSince} read
     * rather than one lookup per rule. Returning the rules themselves (not just
     * their counts) lets {@link UsageCommand#reserve} evaluate and (for alerting)
     * report against them without a second, ungated read.
     */
    ReserveState reserveState(UsageCounterName counter, NaturalistName naturalist, Instant now);

    /**
     * Read model shaping {@link #reserveState}: the current usage count for every
     * active rule on the counter, in no particular order.
     */
    record ReserveState(List<CounterUsage> counters) implements ReadModel {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.observableCollection(counters, "counters");
        }
    }

    /** One rule paired with how many events currently count against it. */
    record CounterUsage(UsageCounter rule, int used) implements ReadModel {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notNull(rule, "rule")
                    .atLeast(used, 0, "used");
        }
    }
}
