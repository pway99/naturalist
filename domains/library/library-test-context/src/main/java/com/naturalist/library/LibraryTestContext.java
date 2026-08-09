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
    private final GlossaryTermQuery glossaryTermQuery;
    private final CitationQuery citationQuery;
    private final CitationAssociationQuery citationAssociationQuery;
    private final CladeQuery cladeQuery;
    private final LibraryCommand libraryCommand;

    private LibraryTestContext(NaturalistDatabase db) {
        ConceptRepository conceptRepository = new ConceptRepositoryMock(db);
        this.conceptQuery = new ConceptQueryImpl(conceptRepository);

        GlossaryTermRepository glossaryTermRepository = new GlossaryTermRepositoryMock(db);
        this.glossaryTermQuery = new GlossaryTermQueryImpl(glossaryTermRepository);

        CitationRepository citationRepository = new CitationRepositoryMock(db);
        this.citationQuery = new CitationQueryImpl(citationRepository);

        CitationAssociationRepository citationAssociationRepository =
                new CitationAssociationRepositoryMock(db);
        this.citationAssociationQuery =
                new CitationAssociationQueryImpl(citationAssociationRepository);

        this.cladeQuery = new CladeQueryImpl();

        LibraryCommand.CitationCommand citationCommand =
                new CitationCommandImpl(citationRepository);
        LibraryCommand.CitationAssociationCommand citationAssociationCommand =
                new CitationAssociationCommandImpl(citationAssociationRepository);
        CitationAttributionTransaction citationAttributionTransaction =
                new CitationAttributionTransaction(
                        citationRepository, citationCommand, citationAssociationCommand);
        this.libraryCommand = new LibraryCommandImpl(
                citationCommand, citationAssociationCommand, citationAttributionTransaction);
    }

    public static LibraryTestContext create(NaturalistDatabase db) {
        return new LibraryTestContext(db);
    }

    public ConceptQuery conceptQuery() {
        return conceptQuery;
    }

    public GlossaryTermQuery glossaryTermQuery() {
        return glossaryTermQuery;
    }

    public CitationQuery citationQuery() {
        return citationQuery;
    }

    public CitationAssociationQuery citationAssociationQuery() {
        return citationAssociationQuery;
    }

    public CladeQuery cladeQuery() {
        return cladeQuery;
    }

    public LibraryCommand libraryCommand() {
        return libraryCommand;
    }
}
