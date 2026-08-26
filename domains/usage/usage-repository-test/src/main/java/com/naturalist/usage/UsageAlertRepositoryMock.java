package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class UsageAlertRepositoryMock
        extends AbstractTestEntityRepository<UsageAlertId, UsageAlert, UsageAlertTestEntitySource>
        implements UsageRepository.AlertRepository {

    UsageAlertRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<UsageAlert> findDedupKey(
            UsageCounterName counter, AlertScope scope, AlertKind kind, String period) {
        observer().arguments("findDedupKey", i -> i
                        .identifier(counter, "counter")
                        .notNull(scope, "scope")
                        .notNull(kind, "kind")
                        .notBlank(period, "period"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.counter().equals(counter)
                        && a.scope() == scope
                        && a.kind() == kind
                        && a.period().equals(period))
                .findFirst();
    }

    @Override
    public List<UsageAlert> getUnacknowledged() {
        return testEntitySource().entityStream()
                .filter(a -> !a.acknowledged())
                .sorted(Comparator.comparing(UsageAlert::at).reversed())
                .toList();
    }

    @Override
    public List<UsageAlert> getUnsent() {
        return testEntitySource().entityStream()
                .filter(a -> !a.emailed())
                .toList();
    }
}
