package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.FamilyCollection;

import java.util.Set;

@DomainService
class InsectFamilyQueryImpl
        extends AbstractEntityQuery<
        InsectFamilyName,
        InsectFamily,
        FamilyCollection,
        InsectRepository.FamilyRepository>
        implements InsectQuery.FamilyQuery {

    InsectFamilyQueryImpl(InsectRepository.FamilyRepository repository) {
        super(repository);
    }

    @Override
    public FamilyCollection findByNameSet(Set<InsectFamilyName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return FamilyCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public FamilyCollection forOrderName(InsectOrderName orderName) {
        observer().arguments("forOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return FamilyCollection.of(repository().getByOrderName(orderName));
    }

    @Override
    public FamilyCollection forOrderNames(Set<InsectOrderName> orderNames) {
        observer().arguments("forOrderNames",
                        i -> i.entityNameCollection(orderNames, "orderNames"))
                .throwWhenInvalid();
        return FamilyCollection.of(repository().getByOrderNames(orderNames));
    }
}
