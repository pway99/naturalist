package com.naturalist.usage;

class UsageTallyRepositoryMockTest implements UsageTallyRepositoryTest {
    @Override
    public UsageRepository.TallyRepository repository() {
        return new UsageTallyRepositoryMock(db);
    }
}
