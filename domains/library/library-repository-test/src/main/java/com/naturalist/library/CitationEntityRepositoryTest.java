package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.net.URI;
import java.util.List;

interface CitationEntityRepositoryTest
        extends EntityRepositoryTest<CitationName, Citation> {

    @Override
    CitationRepository repository();

    @Override
    default TestEntitySource<CitationName, Citation> source() {
        return db.getNamed(CitationTestEntitySource.class);
    }

    @Override
    default CitationName notFoundName() {
        return TestLibraryIdentifiers.Citations.NotFound.name;
    }

    @Override
    default List<CitationName> knownEntityNames() {
        return List.of(
                TestLibraryIdentifiers.Citations.EolSwallowtail,
                TestLibraryIdentifiers.Citations.EolGreenLacewing
        );
    }

    @Override
    default Citation newEntity() {
        return new OnlineSource(
                CitationName.of(RandomValue.string()),
                new AuthorityReference(
                        new AuthoritySource("test", "Test Authority"),
                        URI.create("https://example.com/" + RandomValue.string())),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.integer(),
                null);
    }

    @Override
    default Citation ghostEntity() {
        return new OnlineSource(
                CitationName.of(RandomValue.string()),
                new AuthorityReference(
                        new AuthoritySource("test", "Test Authority"),
                        URI.create("https://example.com/" + RandomValue.string())),
                RandomValue.string(),
                null, null, null);
    }

    @Override
    default Citation modifiedEntity(Citation original) {
        return new OnlineSource(
                original.name(),
                new AuthorityReference(
                        new AuthoritySource("modified", "Modified Authority"),
                        URI.create("https://modified.com/" + RandomValue.string())),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.integer(),
                null);
    }
}
