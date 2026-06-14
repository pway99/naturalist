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

    private LibraryTestContext(NaturalistDatabase db) {
        ConceptRepository repository = new ConceptRepositoryMock(db);
        this.conceptQuery = new ConceptQueryImpl(repository);
    }

    public static LibraryTestContext create(NaturalistDatabase db) {
        return new LibraryTestContext(db);
    }

    public ConceptQuery conceptQuery() {
        return conceptQuery;
    }
}
