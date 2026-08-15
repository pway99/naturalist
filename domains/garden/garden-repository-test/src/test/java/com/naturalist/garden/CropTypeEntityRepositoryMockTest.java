package com.naturalist.garden;

class CropTypeEntityRepositoryMockTest implements CropTypeEntityRepositoryTest {

    @Override
    public CropTypeRepository repository() {
        return new CropTypeEntityRepositoryMock(db);
    }
}
