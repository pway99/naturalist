package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;

import java.time.Clock;

/**
 * Test wiring for {@code usage-core} tests. Follows the same pattern as
 * {@code InsectsTestContextInternal} in {@code insects-core} — constructs the
 * package-private mock repositories from {@code usage-repository-test} within
 * the shared {@code com.naturalist.usage} namespace, then exposes the production
 * {@link UsageQueryImpl} / {@link UsageCommandImpl} pair plus the mocks a test
 * needs to assert against directly (alert dedup, seeded counter rules, raw events).
 */
class UsageCoreTestContext {

    private final UsageQuery query;
    private final UsageCommand command;
    private final UsageRepository.CounterRepository counters;
    private final UsageRepository.EventRepository events;
    private final UsageRepository.AlertRepository alerts;

    private UsageCoreTestContext(NaturalistDatabase db, EntitlementLookup entitlements,
                                  int warningPercent, Clock clock) {
        this.counters = new UsageCounterRepositoryMock(db);
        this.events = new UsageEventRepositoryMock(db);
        this.alerts = new UsageAlertRepositoryMock(db);
        this.query = new UsageQueryImpl(new WarningPercent(warningPercent), clock, counters, events, alerts);
        this.command = new UsageCommandImpl(query, entitlements, new WarningPercent(warningPercent), clock, events, alerts);
    }

    static UsageCoreTestContext create(NaturalistDatabase db, int warningPercent, Clock clock) {
        return new UsageCoreTestContext(db, EntitlementLookup.none(), warningPercent, clock);
    }

    static UsageCoreTestContext create(NaturalistDatabase db, EntitlementLookup entitlements,
                                        int warningPercent, Clock clock) {
        return new UsageCoreTestContext(db, entitlements, warningPercent, clock);
    }

    UsageQuery query() {
        return query;
    }

    UsageCommand command() {
        return command;
    }

    UsageRepository.CounterRepository counters() {
        return counters;
    }

    UsageRepository.EventRepository events() {
        return events;
    }

    UsageRepository.AlertRepository alerts() {
        return alerts;
    }
}
