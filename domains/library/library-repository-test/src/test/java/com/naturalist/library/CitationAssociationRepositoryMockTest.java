package com.naturalist.library;

class CitationAssociationRepositoryMockTest implements CitationAssociationEntityRepositoryTest {
    @Override
    public CitationAssociationRepository repository() {
        return new CitationAssociationRepositoryMock(db);
    }
}
