package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;

import java.time.Clock;

/**
 * Test wiring for {@code usage-core} tests. Follows the same pattern as
 * {@code InsectsTestContextInternal} in {@code insects-core} — constructs the
 * package-private mock repositories from {@code usage-repository-test} within
 * the shared {@code com.naturalist.usage} namespace, then exposes the production
 * {@link UsageQueryImpl} / {@link UsageCommandImpl} pair plus the mocks a test
 * needs to assert against directly (alert dedup, tally state).
 */
class UsageCoreTestContext {

    private final UsageQuery query;
    private final UsageCommand command;
    private final UsageRepository.TallyRepository tallyRepository;
    private final UsageRepository.AlertRepository alertRepository;

    private UsageCoreTestContext(NaturalistDatabase db, UsageLimits limits, Clock clock) {
        this.tallyRepository = new UsageTallyRepositoryMock(db);
        this.alertRepository = new UsageAlertRepositoryMock(db);
        this.query = new UsageQueryImpl(limits, clock, tallyRepository, alertRepository);
        this.command = new UsageCommandImpl(query, limits, clock, tallyRepository, alertRepository);
    }

    static UsageCoreTestContext create(NaturalistDatabase db, UsageLimits limits, Clock clock) {
        return new UsageCoreTestContext(db, limits, clock);
    }

    UsageQuery query() {
        return query;
    }

    UsageCommand command() {
        return command;
    }

    UsageRepository.TallyRepository tallyRepository() {
        return tallyRepository;
    }

    UsageRepository.AlertRepository alertRepository() {
        return alertRepository;
    }
}
