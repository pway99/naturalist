package com.naturalist.naturalist;

class NaturalistEntityRepositoryMockTest implements NaturalistEntityRepositoryTest {

    @Override
    public NaturalistRepository.NaturalistEntityRepository repository() {
        return new NaturalistEntityRepositoryMock(db);
    }
}
