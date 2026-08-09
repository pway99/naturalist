package com.naturalist.library;

class GlossaryTermEntityRepositoryMockTest implements GlossaryTermEntityRepositoryTest {
    @Override
    public GlossaryTermRepository repository() {
        return new GlossaryTermRepositoryMock(db);
    }
}
