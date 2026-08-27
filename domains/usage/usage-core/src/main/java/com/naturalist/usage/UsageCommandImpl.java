package com.naturalist.usage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Write-side adapter for {@link UsageCommand} — the mutation half of what was a
 * single combined {@code UsageBudgetService}, now over an append-only event log
 * rather than upserted tally rows. {@link #reserve(NaturalistName)} consults
 * {@link UsageQuery#reserveState} (the ONE gated, batched read — see
 * {@link UsageQueryImpl}) for every active rule on the {@code identification}
 * counter, applies the same all-or-nothing check against each rule's count,
 * then — if every rule passes — inserts one {@link UsageEvent}. There is no
 * upsert: the event just inserted is exactly what the next read counts.
 *
 * <p>An {@link EntitlementLookup#isEntitled entitled} naturalist bypasses the
 * public-bucket rules entirely (today: nobody, via {@link EntitlementLookup#none()}
 * — the seam exists for a future credit-balance check).
 *
 * <p><b>Every method here is {@code synchronized} on this instance.</b> The reserve
 * algorithm is check-then-insert across several repository round trips
 * (including the {@code query.reserveState} call), so the synchronization
 * boundary — not the in-memory mock or a future RDBMS adapter — is what prevents
 * two concurrent callers from both observing {@code used == limit - 1} and both
 * inserting past the limit. {@code acknowledge} and {@code claimUnsentAlerts}
 * are synchronized alongside it for the same reason the original combined service
 * synchronized every method: they share the same alert-repository state
 * {@code reserve}'s warning/hard-stop dedup writes to.
 *
 * <p><b>The injected {@link Clock} is expected to be UTC-zoned</b> — see
 * {@link UsageWindows} for why.
 */
@DomainService
class UsageCommandImpl implements UsageCommand {

    private static final UsageCounterName COUNTER = UsageCounterName.of("identification");

    private final Observer observer = Observer.forClass(getClass());
    private final UsageQuery query;
    private final EntitlementLookup entitlements;
    private final int warningPercent;
    private final Clock clock;
    private final UsageRepository.EventRepository events;
    private final UsageRepository.AlertRepository alerts;

    UsageCommandImpl(UsageQuery query,
                      EntitlementLookup entitlements,
                      WarningPercent warningPercent,
                      Clock clock,
                      UsageRepository.EventRepository events,
                      UsageRepository.AlertRepository alerts) {
        observer.arguments("constructor", i -> i
                        .notNull(query, "query")
                        .notNull(entitlements, "entitlements")
                        .notNull(warningPercent, "warningPercent")
                        .notNull(clock, "clock")
                        .notNull(events, "events")
                        .notNull(alerts, "alerts"))
                .throwWhenInvalid();
        this.query = query;
        this.entitlements = entitlements;
        this.warningPercent = warningPercent.value();
        this.clock = clock;
        this.events = events;
        this.alerts = alerts;
    }

    @Override
    public synchronized void reserve(NaturalistName naturalist) {
        observer.arguments("reserve", i -> i.identifier(naturalist, "naturalist")).throwWhenInvalid();
        Instant now = clock.instant();

        if (!entitlements.isEntitled(naturalist)) {                 // PUBLIC bucket
            UsageQuery.ReserveState state = query.reserveState(COUNTER, naturalist, now);
            for (UsageQuery.CounterUsage cu : state.counters()) {
                UsageCounter rule = cu.rule();
                if (cu.used() >= rule.limit()) {
                    if (rule.scope() == UsageScope.GLOBAL) {
                        recordAlert(AlertKind.HARD_STOP, rule, cu.used(), now);
                    }
                    throw new BudgetExceededException(limitKindOf(rule), UsageWindows.resetAt(rule, now));
                }
                if (rule.scope() == UsageScope.GLOBAL
                        && cu.used() + 1 == UsagePolicy.warningThreshold(rule.limit(), warningPercent)) {
                    recordAlert(AlertKind.WARNING, rule, cu.used() + 1, now);
                }
            }
        }
        // ENTITLED bucket falls straight through (future: credit-balance check).
        events.insert(new UsageEvent(UsageEventId.create(), COUNTER, naturalist, now));
    }

    private static LimitKind limitKindOf(UsageCounter rule) {
        if (rule.scope() == UsageScope.PER_USER) {
            return LimitKind.PER_USER;
        }
        return rule.windowKind() == WindowKind.CALENDAR_DAY ? LimitKind.DAILY : LimitKind.MONTHLY;
    }

    private void recordAlert(AlertKind kind, UsageCounter rule, int used, Instant now) {
        AlertScope scope = UsageWindows.alertScope(rule);
        String period = UsageWindows.alertPeriod(rule, now);
        if (alerts.findDedupKey(COUNTER, scope, kind, period).isEmpty()) {
            alerts.insert(new UsageAlert(
                    UsageAlertId.create(), COUNTER, scope, kind, period,
                    UsagePolicy.alertMessage(scope, kind, used, rule.limit()),
                    now, false, false));
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
