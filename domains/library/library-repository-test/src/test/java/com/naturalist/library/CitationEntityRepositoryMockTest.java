package com.naturalist.library;

class CitationEntityRepositoryMockTest implements CitationEntityRepositoryTest {
    @Override
    public CitationRepository repository() {
        return new CitationRepositoryMock(db);
    }
}
