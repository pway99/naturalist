package com.naturalist.naturalist;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class NaturalistCredentialRepositoryMock
        extends AbstractTestEntityRepository<NaturalistName, NaturalistCredential, NaturalistCredentialTestEntitySource>
        implements NaturalistRepository.CredentialRepository {

    NaturalistCredentialRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
