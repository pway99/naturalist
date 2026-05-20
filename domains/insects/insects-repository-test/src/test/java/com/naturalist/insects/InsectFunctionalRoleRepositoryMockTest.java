package com.naturalist.insects;

class InsectFunctionalRoleRepositoryMockTest implements InsectFunctionalRoleEntityRepositoryTest {
    @Override
    public InsectRepository.FunctionalRoleRepository repository() {
        return new InsectFunctionalRoleRepositoryMock(db);
    }
}
