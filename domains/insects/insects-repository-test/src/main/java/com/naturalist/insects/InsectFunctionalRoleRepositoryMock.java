package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@MockDomainService
class InsectFunctionalRoleRepositoryMock
        extends AbstractTestEntityRepository<InsectFunctionalRoleId, InsectFunctionalRole, InsectFunctionalRoleTestEntitySource>
        implements InsectRepository.FunctionalRoleRepository {

    InsectFunctionalRoleRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFunctionalRole> getByGuild(FunctionalGuild guild) {
        MethodObserver getByGuild = observer().forMethod("getByGuild");
        getByGuild.arguments(i -> i.notNull(guild, "guild"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(role -> role.guilds().contains(guild))
                .toList();
        getByGuild.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName) {
        MethodObserver getByParentName = observer().forMethod("getByParentName");
        getByParentName.arguments(i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(role -> role.parentName().equals(parentName))
                .findFirst();
        getByParentName.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectFunctionalRole> getByParentNames(Set<InsectRankName> parentNames) {
        MethodObserver getByParentNames = observer().forMethod("getByParentNames");
        getByParentNames.arguments(i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(role -> parentNames.contains(role.parentName()))
                .toList();
        getByParentNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
