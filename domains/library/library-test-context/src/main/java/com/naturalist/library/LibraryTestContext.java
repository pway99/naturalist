package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;

/**
 * Pre-wired, in-memory read surface for the library bounded context. Mirrors
 * {@code ChemistryTestContext} — colocated in {@code com.naturalist.library}
 * so it can assemble the package-private {@link ConceptRepositoryMock} and the
 * package-private {@code ConceptQueryImpl} without promoting either to public.
 *
 * <p>Read seam only. Not a JUnit extension — consumers needing per-method reset
 * wrap a {@code NaturalistDatabaseExtension} alongside this context.
 */
public class LibraryTestContext {

    private final ConceptQuery conceptQuery;
    private final CitationQuery citationQuery;
    private final CitationAssociationQuery citationAssociationQuery;

    private LibraryTestContext(NaturalistDatabase db) {
        ConceptRepository conceptRepository = new ConceptRepositoryMock(db);
        this.conceptQuery = new ConceptQueryImpl(conceptRepository);

        CitationRepository citationRepository = new CitationRepositoryMock(db);
        this.citationQuery = new CitationQueryImpl(citationRepository);

        CitationAssociationRepository citationAssociationRepository =
                new CitationAssociationRepositoryMock(db);
        this.citationAssociationQuery =
                new CitationAssociationQueryImpl(citationAssociationRepository);
    }

    public static LibraryTestContext create(NaturalistDatabase db) {
        return new LibraryTestContext(db);
    }

    public ConceptQuery conceptQuery() {
        return conceptQuery;
    }

    public CitationQuery citationQuery() {
        return citationQuery;
    }

    public CitationAssociationQuery citationAssociationQuery() {
        return citationAssociationQuery;
    }
}
