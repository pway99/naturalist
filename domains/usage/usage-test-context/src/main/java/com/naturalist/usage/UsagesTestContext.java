package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;

import java.time.Clock;

/**
 * Published test wiring for the usage bounded context — the console-consumer
 * counterpart to {@code usage-core}'s internal {@code UsageCoreTestContext}.
 * Colocates into {@code com.naturalist.usage} so it can assemble the
 * package-private mock repositories ({@code usage-repository-test}) and the
 * {@code *Impl} adapters ({@code usage-core}) across the split package without
 * promoting any of them to public.
 *
 * <p>Exposes the production {@link UsageQueryImpl} / {@link UsageCommandImpl} /
 * {@link IdentificationBudgetImpl} over one shared {@link NaturalistDatabase}, so
 * a slice test can reserve budget through {@link #identificationBudget()} and read
 * the result back through {@link #usageQuery()} — the same shared-state coupling
 * the production trio has. {@code warningPercent} and {@code clock} are supplied by
 * the caller (a fixed clock keeps window-based snapshots deterministic).
 */
public class UsagesTestContext {

    private final UsageQuery query;
    private final UsageCommand command;
    private final IdentificationBudget budget;

    private UsagesTestContext(NaturalistDatabase db, int warningPercent, Clock clock) {
        UsageRepository.CounterRepository counters = new UsageCounterRepositoryMock(db);
        UsageRepository.EventRepository events = new UsageEventRepositoryMock(db);
        UsageRepository.AlertRepository alerts = new UsageAlertRepositoryMock(db);
        this.query = new UsageQueryImpl(new WarningPercent(warningPercent), clock, counters, events, alerts);
        this.command = new UsageCommandImpl(
                query, EntitlementLookup.none(), new WarningPercent(warningPercent), clock, events, alerts);
        this.budget = new IdentificationBudgetImpl(command);
    }

    public static UsagesTestContext create(NaturalistDatabase db, int warningPercent, Clock clock) {
        return new UsagesTestContext(db, warningPercent, clock);
    }

    public UsageQuery usageQuery() {
        return query;
    }

    public UsageCommand usageCommand() {
        return command;
    }

    public IdentificationBudget identificationBudget() {
        return budget;
    }
}
