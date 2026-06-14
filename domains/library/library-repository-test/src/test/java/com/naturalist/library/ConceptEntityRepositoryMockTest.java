package com.naturalist.library;

class ConceptEntityRepositoryMockTest implements ConceptEntityRepositoryTest {
    @Override
    public ConceptRepository repository() {
        return new ConceptRepositoryMock(db);
    }
}
