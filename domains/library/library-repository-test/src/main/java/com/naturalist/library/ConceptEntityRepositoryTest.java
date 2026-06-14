package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;

import java.util.List;

interface ConceptEntityRepositoryTest
        extends EntityRepositoryTest<ConceptName, Concept> {

    @Override
    ConceptRepository repository();

    @Override
    default TestEntitySource<ConceptName, Concept> source() {
        return db.getNamed(ConceptTestEntitySource.class);
    }

    @Override
    default ConceptName notFoundName() {
        return TestLibraryIdentifiers.Concepts.NotFound.name;
    }

    @Override
    default List<ConceptName> knownEntityNames() {
        return List.of(
                TestLibraryIdentifiers.Concepts.Clade,
                TestLibraryIdentifiers.Concepts.CladeTaxonomyRelation
        );
    }

    @Override
    default Concept newEntity() {
        return new Concept(
                ConceptName.of(RandomValue.string()),
                RandomValue.string(),
                description());
    }

    @Override
    default Concept ghostEntity() {
        return new Concept(
                ConceptName.of(RandomValue.string()),
                RandomValue.string(),
                description());
    }

    @Override
    default Concept modifiedEntity(Concept original) {
        return new Concept(
                original.name(),
                RandomValue.string(),
                description());
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
