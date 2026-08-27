package com.naturalist.usage;

class UsageEventRepositoryMockTest implements UsageEventRepositoryTest {
    @Override
    public UsageRepository.EventRepository repository() {
        return new UsageEventRepositoryMock(db);
    }
}
