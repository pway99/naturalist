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
        observer().arguments("getByGuild",
                        i -> i.notNull(guild, "guild"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(role -> role.guilds().contains(guild))
                .toList();
    }

    @Override
    public Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName) {
        observer().arguments("getByParentName",
                        i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(role -> role.parentName().equals(parentName))
                .findFirst();
    }
}
