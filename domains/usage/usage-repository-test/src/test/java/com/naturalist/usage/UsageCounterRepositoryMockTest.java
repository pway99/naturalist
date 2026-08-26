package com.naturalist.usage;

class UsageCounterRepositoryMockTest implements UsageCounterRepositoryTest {
    @Override
    public UsageRepository.CounterRepository repository() {
        return new UsageCounterRepositoryMock(db);
    }
}
