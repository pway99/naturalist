package com.naturalist.chemistry.compound;

class CompoundEntityRepositoryMockTest implements CompoundEntityRepositoryTest {
    @Override
    public CompoundRepository.CompoundEntityRepository repository() {
        return new CompoundEntityRepositoryMock(db);
    }
}
