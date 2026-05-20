package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;
import java.util.Optional;

@DomainService
class InsectFunctionalRoleRepositoryMock
        extends AbstractTestEntityRepository<InsectFunctionalRoleId, InsectFunctionalRole, InsectFunctionalRoleTestEntitySource>
        implements InsectRepository.FunctionalRoleRepository {

    InsectFunctionalRoleRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFunctionalRole> getByGuild(FunctionalGuild guild) {
        return testEntitySource().entityStream()
                .filter(role -> role.guilds().contains(guild))
                .toList();
    }

    @Override
    public Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName) {
        return testEntitySource().entityStream()
                .filter(role -> role.parentName().equals(parentName))
                .findFirst();
    }
}
