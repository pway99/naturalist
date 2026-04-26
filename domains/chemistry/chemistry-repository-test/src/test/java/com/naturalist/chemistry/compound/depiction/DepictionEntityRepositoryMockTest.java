package com.naturalist.chemistry.compound.depiction;

class DepictionEntityRepositoryMockTest implements DepictionEntityRepositoryTest {
    @Override
    public DepictionRepository.DepictionEntityRepository repository() {
        return new DepictionEntityRepositoryMock(db);
    }
}
