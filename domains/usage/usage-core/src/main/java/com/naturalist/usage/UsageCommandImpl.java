package com.naturalist.usage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Write-side adapter for {@link UsageCommand} — the mutation half of what was a
 * single combined {@code UsageBudgetService}. {@link #reserve(NaturalistName)}
 * consults {@link UsageQuery#reserveState(NaturalistName)} (the ONE gated,
 * batched read — see {@link UsageQueryImpl}) for the four relevant tally rows,
 * applies the same all-or-nothing check against their counts, then upserts those
 * SAME rows and records threshold alerts (deduplicated). There is no second read:
 * the rows {@code reserveState} returns are exactly what the upsert writes back.
 *
 * <p><b>Every method here is {@code synchronized} on this instance.</b> The reserve
 * algorithm is check-then-increment across several repository round trips
 * (including the {@code query.reserveState} call), so the synchronization
 * boundary — not the in-memory mock or a future RDBMS adapter — is what prevents
 * two concurrent callers from both observing {@code count == limit - 1} and both
 * incrementing past the limit. {@code acknowledge} and {@code claimUnsentAlerts}
 * are synchronized alongside it for the same reason the original combined service
 * synchronized every method: they share the same alert-repository state
 * {@code reserve}'s warning/hard-stop dedup writes to.
 *
 * <p><b>The injected {@link Clock} is expected to be UTC-zoned</b> — see
 * {@link UsagePeriods} for why.
 */
@DomainService
class UsageCommandImpl implements UsageCommand {

    private final Observer observer = Observer.forClass(getClass());
    private final UsageQuery query;
    private final UsageLimits limits;
    private final Clock clock;
    private final UsageRepository.TallyRepository tallies;
    private final UsageRepository.AlertRepository alerts;
    private final UsageCounterName counter = UsageCounterName.of("identification");

    UsageCommandImpl(UsageQuery query,
                      UsageLimits limits,
                      Clock clock,
                      UsageRepository.TallyRepository tallies,
                      UsageRepository.AlertRepository alerts) {
        observer.arguments("constructor", i -> i
                        .notNull(query, "query")
                        .notNull(limits, "limits")
                        .notNull(clock, "clock")
                        .notNull(tallies, "tallies")
                        .notNull(alerts, "alerts"))
                .throwWhenInvalid();
        this.query = query;
        this.limits = limits;
        this.clock = clock;
        this.tallies = tallies;
        this.alerts = alerts;
    }

    @Override
    public synchronized void reserve(NaturalistName naturalist) {
        observer.arguments("reserve", i -> i.identifier(naturalist, "naturalist"))
                .throwWhenInvalid();

        UsagePeriods periods = UsagePeriods.now(clock);
        UsageQuery.ReserveState state = query.reserveState(naturalist);

        Optional<UsageTally> monthlyTally = state.globalMonthly();
        Optional<UsageTally> dailyTally = state.globalDaily();
        Optional<UsageTally> rateTally = state.globalRate();
        Optional<UsageTally> userTally = state.userDaily();

        int monthlyCount = monthlyTally.map(UsageTally::count).orElse(0);
        int dailyCount = dailyTally.map(UsageTally::count).orElse(0);
        int rateCount = rateTally.map(UsageTally::count).orElse(0);
        int userCount = userTally.map(UsageTally::count).orElse(0);

        if (monthlyCount >= limits.globalMonthly()) {
            recordAlert(AlertKind.HARD_STOP, AlertScope.MONTHLY, periods.monthlySlug, monthlyCount, limits.globalMonthly());
            throw new BudgetExceededException(LimitKind.MONTHLY, periods.startOfNextMonth());
        }
        if (dailyCount >= limits.globalDaily()) {
            recordAlert(AlertKind.HARD_STOP, AlertScope.DAILY, periods.dailySlug, dailyCount, limits.globalDaily());
            throw new BudgetExceededException(LimitKind.DAILY, periods.startOfNextDay());
        }
        if (rateCount >= limits.globalRatePerMinute()) {
            throw new BudgetExceededException(LimitKind.RATE, periods.startOfNextMinute());
        }
        if (userCount >= limits.perUserDaily()) {
            throw new BudgetExceededException(LimitKind.PER_USER, periods.startOfNextDay());
        }

        // Upsert the SAME rows reserveState() returned — no second read. Safe: this
        // whole method is synchronized, so no write has happened since reserveState()
        // observed this state above.
        upsert(monthlyTally, null, periods.monthlySlug, monthlyCount + 1);
        checkWarning(monthlyCount + 1, limits.globalMonthly(), AlertScope.MONTHLY, periods.monthlySlug);

        upsert(dailyTally, null, periods.dailySlug, dailyCount + 1);
        checkWarning(dailyCount + 1, limits.globalDaily(), AlertScope.DAILY, periods.dailySlug);

        upsert(rateTally, null, periods.rateSlug, rateCount + 1);

        upsert(userTally, naturalist, periods.dailySlug, userCount + 1);
    }

    private void upsert(Optional<UsageTally> existing, @Nullable NaturalistName naturalist, String period, int newCount) {
        if (existing.isPresent()) {
            tallies.save(existing.get().withCount(newCount));
        } else {
            tallies.insert(new UsageTally(UsageTallyId.create(), counter, naturalist, period, newCount));
        }
    }

    private void checkWarning(int newCount, int limit, AlertScope scope, String period) {
        if (newCount == UsagePolicy.warningThreshold(limit, limits.warningPercent())) {
            recordAlert(AlertKind.WARNING, scope, period, newCount, limit);
        }
    }

    private void recordAlert(AlertKind kind, AlertScope scope, String period, int used, int limit) {
        if (alerts.findDedupKey(counter, scope, kind, period).isEmpty()) {
            alerts.insert(new UsageAlert(
                    UsageAlertId.create(), counter, scope, kind, period,
                    UsagePolicy.alertMessage(scope, kind, used, limit),
                    clock.instant(), false, false));
        }
    }

    @Override
    public synchronized void acknowledge(UsageAlertId id) {
        observer.arguments("acknowledge", i -> i.entityId(id, "id"))
                .throwWhenInvalid();
        UsageAlert alert = alerts.getByName(id).orElseThrow();
        alerts.save(alert.withAcknowledged(true));
    }

    @Override
    public synchronized List<UsageAlert> claimUnsentAlerts() {
        List<UsageAlert> unsent = alerts.getUnsent();
        List<UsageAlert> claimed = new ArrayList<>();
        for (UsageAlert alert : unsent) {
            alerts.save(alert.withEmailed(true));
            claimed.add(alert);
        }
        return claimed;
    }
}
