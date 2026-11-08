package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;

@MockDomainService
class UsageCounterRepositoryMock
        extends AbstractTestEntityRepository<UsageCounterId, UsageCounter, UsageCounterTestEntitySource>
        implements UsageRepository.CounterRepository {

    UsageCounterRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<UsageCounter> findByCounterName(UsageCounterName counterName) {
        MethodObserver findByCounterName = observer().forMethod("findByCounterName");
        findByCounterName.arguments(i -> i.identifier(counterName, "counterName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(c -> c.counterName().equals(counterName))
                .toList();
        findByCounterName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
