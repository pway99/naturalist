package com.naturalist.usage;

import com.naturalist.ddd.ReadModel;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * Read port over the current identification-usage picture — the CQS query half of
 * what was a single combined read/write service. Aggregate display state
 * ({@link #snapshot()}), the alert list ({@link #activeAlerts()}), and the batched
 * tally counts {@link UsageCommand#reserve} needs to evaluate its all-or-nothing
 * check ({@link #reserveCounts(NaturalistName)}).
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
     * The four tally counts {@link UsageCommand#reserve} checks — global monthly,
     * global daily, global rate, and this naturalist's per-user daily — resolved in
     * ONE batched {@code findByCounterAndPeriods} read rather than one lookup per
     * limit.
     */
    ReserveCounts reserveCounts(NaturalistName naturalist);

    /**
     * Read model shaping {@link #reserveCounts}: the current count for each of the
     * four limits {@link UsageCommand#reserve} evaluates, in the same order.
     */
    record ReserveCounts(int globalMonthly, int globalDaily, int globalRate, int userDaily) implements ReadModel {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .atLeast(globalMonthly, 0, "globalMonthly")
                    .atLeast(globalDaily, 0, "globalDaily")
                    .atLeast(globalRate, 0, "globalRate")
                    .atLeast(userDaily, 0, "userDaily");
        }
    }
}
