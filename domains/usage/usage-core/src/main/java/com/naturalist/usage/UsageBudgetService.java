package com.naturalist.usage;

import com.naturalist.data.Pages;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@code usage-core}'s single load-bearing service — the reserve-side write
 * path ({@link IdentificationBudget}) and the read-side monitor
 * ({@link UsageMonitor}) over the same repository-backed tallies and alerts.
 *
 * <p>{@link #reserve(NaturalistName)} checks four limits in order — MONTHLY,
 * DAILY, RATE, PER_USER — all-or-nothing: the first exceeded limit rejects the
 * whole reservation and increments nothing. Every method here is
 * {@code synchronized} on this instance: the reserve algorithm is
 * check-then-increment across several repository round trips, so the
 * synchronization boundary — not the in-memory mock or a future RDBMS
 * adapter — is what prevents two concurrent callers from both observing
 * {@code count == limit - 1} and both incrementing past the limit.
 *
 * <p><b>The injected {@link Clock} is expected to be UTC-zoned.</b> Period
 * buckets ({@code dailySlug}, {@code monthlySlug}, {@code rateSlug}) are
 * derived via {@code LocalDate}/{@code YearMonth}/{@code LocalDateTime}
 * {@code .now(clock)}, and every {@code BudgetExceededException}'s
 * {@code resetAt} is then computed by anchoring those values to
 * {@link ZoneOffset#UTC} ({@code startOfNextDay}/{@code startOfNextMonth}/
 * {@code startOfNextMinute}). A non-UTC clock would shift which wall-clock
 * moment a bucket rolls over at, desynchronizing the reset boundary from the
 * bucket it was derived from. The production bean supplies
 * {@code Clock.systemUTC()}.
 */
@DomainService
class UsageBudgetService implements IdentificationBudget, UsageMonitor {

    private static final DateTimeFormatter RATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");

    private final UsageLimits limits;
    private final Clock clock;
    private final UsageRepository.CounterRepository counters;
    private final UsageRepository.TallyRepository tallies;
    private final UsageRepository.AlertRepository alerts;
    private final UsageCounterName counter = UsageCounterName.of("identification");

    UsageBudgetService(UsageLimits limits,
                        Clock clock,
                        UsageRepository.CounterRepository counters,
                        UsageRepository.TallyRepository tallies,
                        UsageRepository.AlertRepository alerts) {
        Observer.forClass(UsageBudgetService.class).arguments("constructor", i -> i
                        .notNull(limits, "limits")
                        .notNull(clock, "clock")
                        .notNull(counters, "counters")
                        .notNull(tallies, "tallies")
                        .notNull(alerts, "alerts"))
                .throwWhenInvalid();
        this.limits = limits;
        this.clock = clock;
        this.counters = counters;
        this.tallies = tallies;
        this.alerts = alerts;
    }

    @Override
    public synchronized void reserve(NaturalistName naturalist) {
        LocalDate day = LocalDate.now(clock);
        YearMonth month = YearMonth.now(clock);
        LocalDateTime minute = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        String dailySlug = "daily-" + day;
        String monthlySlug = "monthly-" + month;
        String rateSlug = "rate-" + minute.format(RATE_FORMAT);

        List<UsageTally> found = tallies.findByCounterAndPeriods(
                counter, Set.of(monthlySlug, dailySlug, rateSlug), naturalist);

        Optional<UsageTally> monthlyTally = findTally(found, null, monthlySlug);
        Optional<UsageTally> dailyTally = findTally(found, null, dailySlug);
        Optional<UsageTally> rateTally = findTally(found, null, rateSlug);
        Optional<UsageTally> userTally = findTally(found, naturalist, dailySlug);

        int monthlyCount = monthlyTally.map(UsageTally::count).orElse(0);
        int dailyCount = dailyTally.map(UsageTally::count).orElse(0);
        int rateCount = rateTally.map(UsageTally::count).orElse(0);
        int userCount = userTally.map(UsageTally::count).orElse(0);

        if (monthlyCount >= limits.globalMonthly()) {
            recordAlert(AlertKind.HARD_STOP, AlertScope.MONTHLY, monthlySlug, monthlyCount, limits.globalMonthly());
            throw new BudgetExceededException(LimitKind.MONTHLY, startOfNextMonth(month));
        }
        if (dailyCount >= limits.globalDaily()) {
            recordAlert(AlertKind.HARD_STOP, AlertScope.DAILY, dailySlug, dailyCount, limits.globalDaily());
            throw new BudgetExceededException(LimitKind.DAILY, startOfNextDay(day));
        }
        if (rateCount >= limits.globalRatePerMinute()) {
            throw new BudgetExceededException(LimitKind.RATE, startOfNextMinute(minute));
        }
        if (userCount >= limits.perUserDaily()) {
            throw new BudgetExceededException(LimitKind.PER_USER, startOfNextDay(day));
        }

        upsert(monthlyTally, null, monthlySlug, monthlyCount + 1);
        checkWarning(monthlyCount + 1, limits.globalMonthly(), AlertScope.MONTHLY, monthlySlug);

        upsert(dailyTally, null, dailySlug, dailyCount + 1);
        checkWarning(dailyCount + 1, limits.globalDaily(), AlertScope.DAILY, dailySlug);

        upsert(rateTally, null, rateSlug, rateCount + 1);

        upsert(userTally, naturalist, dailySlug, userCount + 1);
    }

    private static Optional<UsageTally> findTally(List<UsageTally> found, @Nullable NaturalistName naturalist, String period) {
        return found.stream()
                .filter(t -> Objects.equals(t.naturalist(), naturalist) && t.period().equals(period))
                .findFirst();
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

    private static Instant startOfNextMonth(YearMonth month) {
        return month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant startOfNextDay(LocalDate day) {
        return day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant startOfNextMinute(LocalDateTime minute) {
        return minute.plusMinutes(1).atZone(ZoneOffset.UTC).toInstant();
    }

    @Override
    public synchronized UsageSnapshot snapshot() {
        LocalDate day = LocalDate.now(clock);
        YearMonth month = YearMonth.now(clock);
        LocalDateTime minute = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        String dailySlug = "daily-" + day;
        String monthlySlug = "monthly-" + month;
        String rateSlug = "rate-" + minute.format(RATE_FORMAT);

        List<UsageTally> globals = tallies.findByCounterAndPeriods(
                counter, Set.of(dailySlug, monthlySlug, rateSlug), null);
        int dailyUsed = findTally(globals, null, dailySlug).map(UsageTally::count).orElse(0);
        int monthlyUsed = findTally(globals, null, monthlySlug).map(UsageTally::count).orElse(0);
        int rateUsed = findTally(globals, null, rateSlug).map(UsageTally::count).orElse(0);

        List<UsageSnapshot.UserUsage> users = Pages.stream(1000, tallies::getPage)
                .filter(t -> t.counter().equals(counter) && t.naturalist() != null && t.period().equals(dailySlug))
                .map(t -> new UsageSnapshot.UserUsage(t.naturalist().value(), t.count(), limits.perUserDaily()))
                .toList();

        return new UsageSnapshot(dailyUsed, limits.globalDaily(), monthlyUsed, limits.globalMonthly(),
                rateUsed, limits.globalRatePerMinute(), users);
    }

    @Override
    public synchronized List<UsageAlert> activeAlerts() {
        return alerts.getUnacknowledged();
    }

    @Override
    public synchronized void acknowledge(UsageAlertId id) {
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
