package com.naturalist.chemistry.element;

class ElementEntityRepositoryMockTest implements ElementEntityRepositoryTest {
    @Override
    public ElementRepository.ElementEntityRepository repository() {
        return new ElemenEntitytRepositoryMock(db);
    }
}
