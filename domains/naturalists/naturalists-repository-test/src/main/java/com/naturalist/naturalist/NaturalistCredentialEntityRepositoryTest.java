package com.naturalist.naturalist;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link NaturalistRepository.CredentialRepository}.
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 */
interface NaturalistCredentialEntityRepositoryTest
        extends EntityRepositoryTest<NaturalistName, NaturalistCredential> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");
    NaturalistName NOT_FOUND = NaturalistName.of("nobody-here");

    @Override
    NaturalistRepository.CredentialRepository repository();

    @Override
    default TestEntitySource<NaturalistName, NaturalistCredential> source() {
        return db.getNamed(NaturalistCredentialTestEntitySource.class);
    }

    @Override
    default NaturalistName notFoundName() {
        return NOT_FOUND;
    }

    @Override
    default List<NaturalistName> knownEntityNames() {
        return List.of(PATRICK, DELIA);
    }

    @Override
    default NaturalistCredential newEntity() {
        return new NaturalistCredential(
                NaturalistName.of(RandomValue.string()),
                "{bcrypt}" + RandomValue.string());
    }

    @Override
    default NaturalistCredential ghostEntity() {
        return new NaturalistCredential(
                NaturalistName.of(RandomValue.string()),
                "{bcrypt}" + RandomValue.string());
    }

    @Override
    default NaturalistCredential modifiedEntity(NaturalistCredential original) {
        return new NaturalistCredential(
                original.name(),
                "{bcrypt}changed-" + RandomValue.string());
    }
}
