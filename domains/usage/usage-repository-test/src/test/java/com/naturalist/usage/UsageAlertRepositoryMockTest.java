package com.naturalist.usage;

class UsageAlertRepositoryMockTest implements UsageAlertRepositoryTest {
    @Override
    public UsageRepository.AlertRepository repository() {
        return new UsageAlertRepositoryMock(db);
    }
}
