package com.naturalist.chemistry.compound;

class DepictionEntityRepositoryMockTest implements DepictionEntityRepositoryTest {
    @Override
    public CompoundRepository.DepictionRepository repository() {
        return new DepictionEntityRepositoryMock(db);
    }
}
