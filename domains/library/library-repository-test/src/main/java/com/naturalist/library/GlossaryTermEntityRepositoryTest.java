package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

interface GlossaryTermEntityRepositoryTest
        extends EntityRepositoryTest<GlossaryTermName, GlossaryTerm> {

    @Override
    GlossaryTermRepository repository();

    @Override
    default TestEntitySource<GlossaryTermName, GlossaryTerm> source() {
        return db.getNamed(GlossaryTermTestEntitySource.class);
    }

    @Override
    default GlossaryTermName notFoundName() {
        return TestLibraryIdentifiers.GlossaryTerms.NotFound.name;
    }

    @Override
    default List<GlossaryTermName> knownEntityNames() {
        return List.of(
                TestLibraryIdentifiers.GlossaryTerms.Conspicuous,
                TestLibraryIdentifiers.GlossaryTerms.Diagnostic
        );
    }

    @Override
    default GlossaryTerm newEntity() {
        return new GlossaryTerm(
                GlossaryTermName.of(RandomValue.string()),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }

    @Override
    default GlossaryTerm ghostEntity() {
        return new GlossaryTerm(
                GlossaryTermName.of(RandomValue.string()),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }

    @Override
    default GlossaryTerm modifiedEntity(GlossaryTerm original) {
        return new GlossaryTerm(
                original.name(),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }
}
