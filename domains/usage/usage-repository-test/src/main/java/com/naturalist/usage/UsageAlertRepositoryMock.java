package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@MockDomainService
class UsageAlertRepositoryMock
        extends AbstractTestEntityRepository<UsageAlertId, UsageAlert, UsageAlertTestEntitySource>
        implements UsageRepository.AlertRepository {

    UsageAlertRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<UsageAlert> findDedupKey(
            UsageCounterName counter, AlertScope scope, AlertKind kind, String period) {
        MethodObserver findDedupKey = observer().forMethod("findDedupKey");
        findDedupKey.arguments(i -> i
                        .identifier(counter, "counter")
                        .notNull(scope, "scope")
                        .notNull(kind, "kind")
                        .notBlank(period, "period"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> a.counter().equals(counter)
                        && a.scope() == scope
                        && a.kind() == kind
                        && a.period().equals(period))
                .findFirst();
        findDedupKey.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<UsageAlert> getUnacknowledged() {
        MethodObserver getUnacknowledged = observer().forMethod("getUnacknowledged");
        var result = testEntitySource().entityStream()
                .filter(a -> !a.acknowledged())
                .sorted(Comparator.comparing(UsageAlert::at).reversed())
                .toList();
        getUnacknowledged.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<UsageAlert> getUnsent() {
        MethodObserver getUnsent = observer().forMethod("getUnsent");
        var result = testEntitySource().entityStream()
                .filter(a -> !a.emailed())
                .toList();
        getUnsent.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
