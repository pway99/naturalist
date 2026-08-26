package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class UsageCounterRepositoryMock
        extends AbstractTestEntityRepository<UsageCounterName, UsageCounter, UsageCounterTestEntitySource>
        implements UsageRepository.CounterRepository {

    UsageCounterRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
