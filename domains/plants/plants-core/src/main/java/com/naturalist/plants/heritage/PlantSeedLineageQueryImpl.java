package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.Set;

@DomainService
class PlantSeedLineageQueryImpl
        extends AbstractEntityQuery<SeedLineageName, SeedLineage, SeedLineageCollection, SeedLineageRepository>
        implements SeedLineageQuery {

    PlantSeedLineageQueryImpl(SeedLineageRepository repository) {
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
    public SeedLineageCollection forCultivarName(CultivarName cultivarName) {
        observer().arguments("forCultivarName", i -> i
                        .entityName(cultivarName, "cultivarName"))
                .throwWhenInvalid();
        return SeedLineageCollection.of(repository().getByCultivarName(cultivarName));
    }
}
