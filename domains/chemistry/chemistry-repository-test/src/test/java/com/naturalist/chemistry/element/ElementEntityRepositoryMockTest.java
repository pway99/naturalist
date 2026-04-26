package com.naturalist.chemistry.element;

class ElementEntityRepositoryMockTest implements ElementEntityRepositoryTest {
    @Override
    public ElementRepository repository() {
        return new ElemenEntitytRepositoryMock(db);
    }
}
