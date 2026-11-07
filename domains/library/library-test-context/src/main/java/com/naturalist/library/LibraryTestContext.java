package com.naturalist.library;

import com.naturalist.test.context.ObjectGraph;
import com.naturalist.data.NaturalistDatabase;

/**
 * Pre-wired, in-memory read and write surface for the library bounded context, for other domains'
 * tests and external adapters. A facade over one {@link ObjectGraph} per {@link #create} call: the
 * wiring lives once, in library-core's {@code LibraryTestContextInternal} and the graph's reflection over
 * the {@code @DomainService} and {@code @MockDomainService} classes, and each accessor hands out what the
 * graph builds, already traced.
 *
 * <p>Not a JUnit extension — consumers needing per-method reset wrap a {@code NaturalistTestExtension}
 * alongside this context.
 */
public class LibraryTestContext {

    private final ObjectGraph graph;

    private LibraryTestContext(NaturalistDatabase db) {
        this.graph = ObjectGraph.create(db);
    }

    public static LibraryTestContext create(NaturalistDatabase db) {
        return new LibraryTestContext(db);
    }

    public ConceptQuery conceptQuery() {
        return graph.get(ConceptQuery.class);
    }

    public GlossaryTermQuery glossaryTermQuery() {
        return graph.get(GlossaryTermQuery.class);
    }

    public CitationQuery citationQuery() {
        return graph.get(CitationQuery.class);
    }

    public CitationAssociationQuery citationAssociationQuery() {
        return graph.get(CitationAssociationQuery.class);
    }

    public CladeQuery cladeQuery() {
        return graph.get(CladeQuery.class);
    }

    public LibraryCommand libraryCommand() {
        return graph.get(LibraryCommand.class);
    }
}
