package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@DomainService
class InsectGenusQueryImpl
        extends AbstractEntityQuery<
        InsectGenusName,
        InsectGenus,
        GenusCollection,
        InsectRepository.GenusRepository>
        implements InsectQuery.GenusQuery {

    private final InsectQuery.FamilyQuery familyQuery;

    InsectGenusQueryImpl(InsectRepository.GenusRepository repository,
                   InsectQuery.FamilyQuery familyQuery) {
        super(repository);
        this.familyQuery = Objects.requireNonNull(familyQuery, "familyQuery");
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

    @Override
    public GenusCollection forFamilyNames(Set<InsectFamilyName> familyNames) {
        observer().arguments("forFamilyNames",
                        i -> i.entityNameCollection(familyNames, "familyNames"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByFamilyNames(familyNames));
    }

    @Override
    public GenusCollection forOrderName(InsectOrderName orderName) {
        observer().arguments("forOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        Set<InsectFamilyName> familyNames = familyQuery.forOrderName(orderName).stream()
                .map(InsectFamily::name)
                .collect(Collectors.toSet());
        return forFamilyNames(familyNames);
    }
}
