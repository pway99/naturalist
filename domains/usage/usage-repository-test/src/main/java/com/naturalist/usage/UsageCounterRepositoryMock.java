package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;

class UsageCounterRepositoryMock
        extends AbstractTestEntityRepository<UsageCounterId, UsageCounter, UsageCounterTestEntitySource>
        implements UsageRepository.CounterRepository {

    UsageCounterRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<UsageCounter> findByCounterName(UsageCounterName counterName) {
        observer().arguments("findByCounterName", i -> i.identifier(counterName, "counterName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(c -> c.counterName().equals(counterName))
                .toList();
    }
}
