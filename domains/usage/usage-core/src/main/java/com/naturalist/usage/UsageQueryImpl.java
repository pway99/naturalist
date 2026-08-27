package com.naturalist.usage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-side adapter for {@link UsageQuery} — modeled on {@code InsectQueryImpl} /
 * {@code InsectCitationQueryImpl} so it is a recognised {@code *QueryImpl}
 * head-of-DAG for the N+1 no-fan-out select gate: each of {@link #snapshot()} and
 * {@link #reserveState} makes exactly two repository selects — one
 * {@code counters.findByCounterName} and one batched
 * {@code events.findByCounterSince} — and counts in memory from there. No
 * per-rule or per-user repository call.
 *
 * <p>Deliberately holds no lock. {@link UsageCommandImpl#reserve} is the only
 * caller that needs read-then-write atomicity, and it gets that by invoking
 * {@link #reserveState} from within its own {@code synchronized} critical
 * section — see that class's javadoc.
 */
@DomainService
class UsageQueryImpl implements UsageQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final int warningPercent;
    private final Clock clock;
    private final UsageRepository.CounterRepository counters;
    private final UsageRepository.EventRepository events;
    private final UsageRepository.AlertRepository alerts;
    private final UsageCounterName counter = UsageCounterName.of("identification");

    UsageQueryImpl(WarningPercent warningPercent,
                   Clock clock,
                   UsageRepository.CounterRepository counters,
                   UsageRepository.EventRepository events,
                   UsageRepository.AlertRepository alerts) {
        observer.arguments("constructor", i -> i
                        .notNull(warningPercent, "warningPercent")
                        .notNull(clock, "clock")
                        .notNull(counters, "counters")
                        .notNull(events, "events")
                        .notNull(alerts, "alerts"))
                .throwWhenInvalid();
        this.warningPercent = warningPercent.value();
        this.clock = clock;
        this.counters = counters;
        this.events = events;
        this.alerts = alerts;
    }

    @Override
    public UsageSnapshot snapshot() {
        Instant now = clock.instant();
        List<UsageCounter> rules = counters.findByCounterName(counter).stream()
                .filter(UsageCounter::active)
                .toList();

        UsageCounter dailyRule = ruleOf(rules, UsageScope.GLOBAL, WindowKind.CALENDAR_DAY);
        UsageCounter monthlyRule = ruleOf(rules, UsageScope.GLOBAL, WindowKind.SINCE);
        UsageCounter userRule = ruleOf(rules, UsageScope.PER_USER, WindowKind.CALENDAR_DAY);

        Instant earliest = rules.stream()
                .map(r -> UsageWindows.windowStart(r, now))
                .min(Comparator.naturalOrder())
                .orElse(now);
        List<UsageEvent> all = events.findByCounterSince(counter, null, earliest);   // ONE gated read

        int dailyUsed = dailyRule == null ? 0 : countSince(all, UsageWindows.windowStart(dailyRule, now));
        int dailyLimit = dailyRule == null ? 0 : dailyRule.limit();
        int monthlyUsed = monthlyRule == null ? 0 : countSince(all, UsageWindows.windowStart(monthlyRule, now));
        int monthlyLimit = monthlyRule == null ? 0 : monthlyRule.limit();
        int userLimit = userRule == null ? 0 : userRule.limit();

        Instant dailyStart = dailyRule == null ? now : UsageWindows.windowStart(dailyRule, now);
        Map<String, Long> byUser = all.stream()
                .filter(e -> !e.instant().isBefore(dailyStart))
                .collect(Collectors.groupingBy(e -> e.naturalist().value(), Collectors.counting()));
        List<UsageSnapshot.UserUsage> users = byUser.entrySet().stream()
                .map(e -> new UsageSnapshot.UserUsage(e.getKey(), e.getValue().intValue(), userLimit))
                .toList();

        return new UsageSnapshot(dailyUsed, dailyLimit, monthlyUsed, monthlyLimit, users);
    }

    @Override
    public List<UsageAlert> activeAlerts() {
        return alerts.getUnacknowledged();
    }

    @Override
    public ReserveState reserveState(UsageCounterName counter, NaturalistName naturalist, Instant now) {
        observer.arguments("reserveState", i -> i
                        .identifier(counter, "counter").identifier(naturalist, "naturalist").notNull(now, "now"))
                .throwWhenInvalid();

        List<UsageCounter> rules = counters.findByCounterName(counter).stream()
                .filter(UsageCounter::active).toList();
        if (rules.isEmpty()) {
            return new ReserveState(List.of());
        }
        Instant earliest = rules.stream()
                .map(r -> UsageWindows.windowStart(r, now))
                .min(Comparator.naturalOrder()).orElse(now);

        List<UsageEvent> all = events.findByCounterSince(counter, null, earliest);   // ONE gated read

        List<CounterUsage> usages = rules.stream()
                .map(r -> new CounterUsage(r, countFor(all, r, naturalist, now)))
                .toList();
        return new ReserveState(usages);
    }

    private static UsageCounter ruleOf(List<UsageCounter> rules, UsageScope scope, WindowKind windowKind) {
        return rules.stream()
                .filter(r -> r.scope() == scope && r.windowKind() == windowKind)
                .findFirst()
                .orElse(null);
    }

    private static int countSince(List<UsageEvent> events, Instant start) {
        return (int) events.stream().filter(e -> !e.instant().isBefore(start)).count();
    }

    private static int countFor(List<UsageEvent> all, UsageCounter rule, NaturalistName naturalist, Instant now) {
        Instant start = UsageWindows.windowStart(rule, now);
        return (int) all.stream()
                .filter(e -> !e.instant().isBefore(start))
                .filter(e -> rule.scope() == UsageScope.GLOBAL || e.naturalist().equals(naturalist))
                .count();
    }
}
