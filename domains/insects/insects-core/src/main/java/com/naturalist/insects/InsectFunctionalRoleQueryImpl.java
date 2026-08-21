package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.FunctionalRoleCollection;

import java.util.Optional;
import java.util.Set;

class InsectFunctionalRoleQueryImpl
        extends AbstractEntityQuery<
        InsectFunctionalRoleId,
        InsectFunctionalRole,
        FunctionalRoleCollection,
        InsectRepository.FunctionalRoleRepository>
        implements InsectQuery.FunctionalRoleQuery {

    InsectFunctionalRoleQueryImpl(InsectRepository.FunctionalRoleRepository repository) {
        super(repository);
    }

    @Override
    public FunctionalRoleCollection findByNameSet(Set<InsectFunctionalRoleId> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return FunctionalRoleCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public FunctionalRoleCollection getByGuild(FunctionalGuild guild) {
        observer().arguments("getByGuild", i -> i.notNull(guild, "guild"))
                .throwWhenInvalid();
        return FunctionalRoleCollection.of(repository().getByGuild(guild));
    }

    @Override
    public Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName) {
        observer().arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return repository().getByParentName(parentName);
    }

    @Override
    public FunctionalRoleCollection getByParentNames(Set<InsectRankName> parentNames) {
        observer().arguments("getByParentNames", i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        return FunctionalRoleCollection.of(repository().getByParentNames(parentNames));
    }
}
