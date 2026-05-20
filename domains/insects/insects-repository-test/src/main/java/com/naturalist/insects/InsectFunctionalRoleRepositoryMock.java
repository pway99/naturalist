package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectFunctionalRoleRepositoryMock
        extends AbstractTestEntityRepository<InsectFunctionalRoleId, InsectFunctionalRole, InsectFunctionalRoleTestEntitySource>
        implements InsectRepository.FunctionalRoleRepository {

    InsectFunctionalRoleRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
