package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class CitationAssociationTestEntitySource
        extends TestEntitySource<CitationAssociationId, CitationAssociation> {

    public CitationAssociationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/citation-associations.json", CitationAssociationJson::parseAll);
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

    @Override
    protected Object writable(CitationAssociation entity) {
        return CitationAssociationJson.fromEntity(entity);
    }

    @Override
    protected Class<?> writableClass() {
        return CitationAssociationJson.class;
    }
}
