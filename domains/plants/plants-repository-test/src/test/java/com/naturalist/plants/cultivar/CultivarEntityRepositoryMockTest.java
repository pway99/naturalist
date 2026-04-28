package com.naturalist.plants.cultivar;

class CultivarEntityRepositoryMockTest implements CultivarEntityRepositoryTest {
    @Override
    public CultivarRepository.CultivarEntityRepository repository() {
        return new CultivarEntityRepositoryMock(db);
    }
}
