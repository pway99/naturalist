package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.FunctionalRoleCollection;

import java.util.Set;

class FunctionalRoleQueryImpl
        extends AbstractEntityQuery<
        InsectFunctionalRoleId,
        InsectFunctionalRole,
        FunctionalRoleCollection,
        InsectRepository.FunctionalRoleRepository>
        implements InsectQuery.FunctionalRoleQuery {

    FunctionalRoleQueryImpl(InsectRepository.FunctionalRoleRepository repository) {
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
}
