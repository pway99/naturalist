package com.naturalist.naturalist;

class NaturalistCredentialRepositoryMockTest implements NaturalistCredentialEntityRepositoryTest {

    @Override
    public NaturalistRepository.CredentialRepository repository() {
        return new NaturalistCredentialRepositoryMock(db);
    }
}
