package com.naturalist.plants.cultivar;

class CultivarRepositoryMockTest implements CultivarRepositoryTest {
    @Override
    public CultivarRepository repository() {
        return new CultivarRepositoryMock(db);
    }
}
