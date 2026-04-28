package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.heritage.SeedLineageEntityCollections.SeedLineageCollection;

import java.util.Set;

@DomainService
class SeedLineageEntityQueryImpl
        extends AbstractEntityQuery<SeedLineageName, SeedLineage, SeedLineageCollection, SeedLineageRepository.SeedLineageEntityRepository>
        implements SeedLineageQuery.SeedLineageEntityQuery {

    SeedLineageEntityQueryImpl(SeedLineageRepository.SeedLineageEntityRepository repository) {
        super(repository);
    }

    @Override
    public SeedLineageCollection findByNameSet(Set<SeedLineageName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return SeedLineageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public EntityNameSet<SeedLineageName> allSeedLineageNames() {
        return EntityNameSet.of(repository().getAllSeedLineageNames());
    }

    @Override
    public SeedLineageCollection forCultivarName(CultivarName cultivarName) {
        observer().arguments("forCultivarName", i -> i
                        .entityName(cultivarName, "cultivarName"))
                .throwWhenInvalid();
        return SeedLineageCollection.of(repository().getByCultivarName(cultivarName));
    }
}
