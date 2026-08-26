package com.naturalist.usage;

import com.naturalist.data.EntityRepository;
import com.naturalist.naturalist.NaturalistName;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

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

        /** Business key: one tally per (counter, naturalist, period). {@code naturalist} is null for a global tally. */
        Optional<UsageTally> findBusinessKey(UsageCounterName counter, @Nullable NaturalistName naturalist, String period);
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
