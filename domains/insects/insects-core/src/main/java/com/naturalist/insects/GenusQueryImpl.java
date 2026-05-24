package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;

import java.util.Set;

@DomainService
class GenusQueryImpl
        extends AbstractEntityQuery<
        InsectGenusName,
        InsectGenus,
        GenusCollection,
        InsectRepository.GenusRepository>
        implements InsectQuery.GenusQuery {

    GenusQueryImpl(InsectRepository.GenusRepository repository) {
        super(repository);
    }

    @Override
    public GenusCollection findByNameSet(Set<InsectGenusName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public GenusCollection forFamilyName(InsectFamilyName familyName) {
        observer().arguments("forFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByFamilyName(familyName));
    }
}
