package com.naturalist.usage;

import com.naturalist.ddd.ReadModel;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Read port over the current identification-usage picture — the CQS query half of
 * what was a single combined read/write service. Aggregate display state
 * ({@link #snapshot()}), the alert list ({@link #activeAlerts()}), and the batched
 * tally rows {@link UsageCommand#reserve} needs to evaluate its all-or-nothing
 * check and to upsert ({@link #reserveState(NaturalistName)}).
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
     * The four tally rows {@link UsageCommand#reserve} checks and upserts — global
     * monthly, global daily, global rate, and this naturalist's per-user daily —
     * resolved in ONE batched {@code findByCounterAndPeriods} read rather than one
     * lookup per limit. Returning the rows themselves (not just their counts) lets
     * the caller upsert against them without a second, ungated read.
     */
    ReserveState reserveState(NaturalistName naturalist);

    /**
     * Read model shaping {@link #reserveState}: the current tally row for each of
     * the four limits {@link UsageCommand#reserve} evaluates, in the same order.
     * Empty when no reservation has been made yet for that period.
     */
    record ReserveState(Optional<UsageTally> globalMonthly, Optional<UsageTally> globalDaily,
                         Optional<UsageTally> globalRate, Optional<UsageTally> userDaily) implements ReadModel {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .namedEntityOrNull(globalMonthly.orElse(null), "globalMonthly")
                    .namedEntityOrNull(globalDaily.orElse(null), "globalDaily")
                    .namedEntityOrNull(globalRate.orElse(null), "globalRate")
                    .namedEntityOrNull(userDaily.orElse(null), "userDaily");
        }
    }
}
