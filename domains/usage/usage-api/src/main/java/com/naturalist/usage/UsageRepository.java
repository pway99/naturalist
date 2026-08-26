package com.naturalist.usage;

import com.naturalist.data.EntityRepository;
import com.naturalist.naturalist.NaturalistName;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Namespace for the usage bounded context's write-side repositories — the single
 * discoverable entry point for persistence of identification-cost-control data.
 *
 * <p>Nested repositories scope to a single entity each:
 * <ul>
 *   <li>{@link CounterRepository} — {@link UsageCounter} entities.</li>
 *   <li>{@link TallyRepository} — {@link UsageTally} entities.</li>
 *   <li>{@link AlertRepository} — {@link UsageAlert} entities.</li>
 * </ul>
 *
 * <p>This is a {@code class}, not an {@code interface}, so the nested repository
 * contracts can carry their own access modifiers. Inside an interface, nested types
 * would be implicitly {@code public static}; inside a class, {@code protected} keeps
 * them hidden from foreign packages while permitting same-package adapter
 * implementations. The class is non-instantiable — it holds no state and no behavior,
 * only the namespace (ADR-020).
 */
class UsageRepository {

    private UsageRepository() {
    }

    protected interface CounterRepository
            extends EntityRepository<UsageCounterName, UsageCounter> {
    }

    protected interface TallyRepository
            extends EntityRepository<UsageTallyId, UsageTally> {

        /**
         * Batched read: every tally for {@code counter} whose {@code period} is in
         * {@code periods} AND is either a global tally ({@code naturalist() == null})
         * or belongs to the given {@code naturalist}. One select in place of one
         * {@code findBusinessKey} call per period. When {@code naturalist} is null,
         * only global tallies are returned (a global tally never equals a null
         * naturalist under {@code equals}, so passing null here can never match a
         * per-user row).
         */
        List<UsageTally> findByCounterAndPeriods(
                UsageCounterName counter, Set<String> periods, @Nullable NaturalistName naturalist);
    }

    protected interface AlertRepository
            extends EntityRepository<UsageAlertId, UsageAlert> {

        /** Dedup key: one alert per (counter, scope, kind, period). */
        Optional<UsageAlert> findDedupKey(UsageCounterName counter, AlertScope scope, AlertKind kind, String period);

        /** Unacknowledged alerts, newest-first by {@link UsageAlert#at()}. */
        List<UsageAlert> getUnacknowledged();

        /** Alerts not yet emailed. */
        List<UsageAlert> getUnsent();
    }
}
