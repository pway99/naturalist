package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class CitationAssociationTestEntitySource
        extends TestEntitySource<CitationAssociationId, CitationAssociation> {

    /**
     * Resolves the cross-domain subject ref both ways for
     * {@link CitationAssociationJson}. This is the mock stack's wiring point —
     * the single place library's in-memory data names a subject domain, and the
     * one line to change when a second subject domain appears.
     */
    private static final EntityRefResolver SUBJECT_RESOLVER = new InsectsEntityRefResolverMock();

    public CitationAssociationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/citation-associations.json",
                json -> CitationAssociationJson.parseAll(json, SUBJECT_RESOLVER));
    }

    @Override
    protected List<UniqueConstraint<CitationAssociation>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "citationName+subject";
                    }

                    @Override
                    public Function<CitationAssociation, ?> valueFunction() {
                        return a -> a.citationName().value() + ":"
                                + a.subject().domain().value() + ":"
                                + a.subject().name().value();
                    }
                });
    }

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on
     * {@code citationName+subject}: the existing row's id is retained, the
     * caller-supplied (typically freshly-minted) id is discarded.
     */
    @Override
    protected CitationAssociation withKey(CitationAssociation a, CitationAssociationId key) {
        return new CitationAssociation(key, a.citationName(), a.subject(), a.note());
    }

    @Override
    protected Object writable(CitationAssociation entity) {
        return CitationAssociationJson.fromEntity(entity, SUBJECT_RESOLVER);
    }

    @Override
    protected Class<?> writableClass() {
        return CitationAssociationJson.class;
    }
}
