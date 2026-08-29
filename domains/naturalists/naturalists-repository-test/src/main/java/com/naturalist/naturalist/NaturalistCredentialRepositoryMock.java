package com.naturalist.naturalist;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class NaturalistCredentialRepositoryMock
        extends AbstractTestEntityRepository<NaturalistName, NaturalistCredential, NaturalistCredentialTestEntitySource>
        implements NaturalistRepository.CredentialRepository {

    NaturalistCredentialRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
