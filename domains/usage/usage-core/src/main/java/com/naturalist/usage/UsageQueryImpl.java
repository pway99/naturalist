package com.naturalist.usage;

import com.naturalist.data.Pages;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-side adapter for {@link UsageQuery} — modeled on {@code InsectQueryImpl} /
 * {@code InsectCitationQueryImpl} so it is a recognised {@code *QueryImpl}
 * head-of-DAG for the N+1 no-fan-out select gate: every batched
 * {@code findByCounterAndPeriods} read this class makes is now gated against
 * fan-out (a property the old combined {@code UsageBudgetService} sidestepped by
 * never being a {@code *QueryImpl}).
 *
 * <p>Deliberately holds no lock. {@link UsageCommandImpl#reserve} is the only
 * caller that needs read-then-write atomicity, and it gets that by invoking
 * {@link #reserveCounts(NaturalistName)} from within its own {@code synchronized}
 * critical section — see that class's javadoc.
 */
@DomainService
class UsageQueryImpl implements UsageQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final UsageLimits limits;
    private final Clock clock;
    private final UsageRepository.TallyRepository tallies;
    private final UsageRepository.AlertRepository alerts;
    private final UsageCounterName counter = UsageCounterName.of("identification");

    UsageQueryImpl(UsageLimits limits,
                   Clock clock,
                   UsageRepository.TallyRepository tallies,
                   UsageRepository.AlertRepository alerts) {
        observer.arguments("constructor", i -> i
                        .notNull(limits, "limits")
                        .notNull(clock, "clock")
                        .notNull(tallies, "tallies")
                        .notNull(alerts, "alerts"))
                .throwWhenInvalid();
        this.limits = limits;
        this.clock = clock;
        this.tallies = tallies;
        this.alerts = alerts;
    }

    @Override
    public UsageSnapshot snapshot() {
        UsagePeriods periods = UsagePeriods.now(clock);

        List<UsageTally> globals = tallies.findByCounterAndPeriods(
                counter, Set.of(periods.dailySlug, periods.monthlySlug, periods.rateSlug), null);
        int dailyUsed = findTally(globals, null, periods.dailySlug).map(UsageTally::count).orElse(0);
        int monthlyUsed = findTally(globals, null, periods.monthlySlug).map(UsageTally::count).orElse(0);
        int rateUsed = findTally(globals, null, periods.rateSlug).map(UsageTally::count).orElse(0);

        List<UsageSnapshot.UserUsage> users = Pages.stream(1000, tallies::getPage)
                .filter(t -> t.counter().equals(counter) && t.naturalist() != null && t.period().equals(periods.dailySlug))
                .map(t -> new UsageSnapshot.UserUsage(t.naturalist().value(), t.count(), limits.perUserDaily()))
                .toList();

        return new UsageSnapshot(dailyUsed, limits.globalDaily(), monthlyUsed, limits.globalMonthly(),
                rateUsed, limits.globalRatePerMinute(), users);
    }

    @Override
    public List<UsageAlert> activeAlerts() {
        return alerts.getUnacknowledged();
    }

    @Override
    public ReserveCounts reserveCounts(NaturalistName naturalist) {
        observer.arguments("reserveCounts", i -> i.identifier(naturalist, "naturalist"))
                .throwWhenInvalid();

        UsagePeriods periods = UsagePeriods.now(clock);
        List<UsageTally> found = tallies.findByCounterAndPeriods(
                counter, Set.of(periods.monthlySlug, periods.dailySlug, periods.rateSlug), naturalist);

        int monthlyCount = findTally(found, null, periods.monthlySlug).map(UsageTally::count).orElse(0);
        int dailyCount = findTally(found, null, periods.dailySlug).map(UsageTally::count).orElse(0);
        int rateCount = findTally(found, null, periods.rateSlug).map(UsageTally::count).orElse(0);
        int userCount = findTally(found, naturalist, periods.dailySlug).map(UsageTally::count).orElse(0);

        return new ReserveCounts(monthlyCount, dailyCount, rateCount, userCount);
    }

    private static Optional<UsageTally> findTally(List<UsageTally> found, @Nullable NaturalistName naturalist, String period) {
        return found.stream()
                .filter(t -> Objects.equals(t.naturalist(), naturalist) && t.period().equals(period))
                .findFirst();
    }
}
