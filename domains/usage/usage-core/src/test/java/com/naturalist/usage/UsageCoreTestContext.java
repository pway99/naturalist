package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;

import java.time.Clock;

/**
 * Test wiring for {@code usage-core} tests. Follows the same pattern as
 * {@code InsectsTestContextInternal} in {@code insects-core} — constructs the
 * package-private mock repositories from {@code usage-repository-test} within
 * the shared {@code com.naturalist.usage} namespace, then exposes the
 * production {@link UsageBudgetService} plus the mocks a test needs to assert
 * against directly (alert dedup, tally state).
 */
class UsageCoreTestContext {

    private final UsageBudgetService service;
    private final UsageRepository.TallyRepository tallyRepository;
    private final UsageRepository.AlertRepository alertRepository;

    private UsageCoreTestContext(NaturalistDatabase db, UsageLimits limits, Clock clock) {
        UsageRepository.CounterRepository counterRepository = new UsageCounterRepositoryMock(db);
        this.tallyRepository = new UsageTallyRepositoryMock(db);
        this.alertRepository = new UsageAlertRepositoryMock(db);
        this.service = new UsageBudgetService(limits, clock, counterRepository, tallyRepository, alertRepository);
    }

    static UsageCoreTestContext create(NaturalistDatabase db, UsageLimits limits, Clock clock) {
        return new UsageCoreTestContext(db, limits, clock);
    }

    UsageBudgetService service() {
        return service;
    }

    UsageRepository.TallyRepository tallyRepository() {
        return tallyRepository;
    }

    UsageRepository.AlertRepository alertRepository() {
        return alertRepository;
    }
}
