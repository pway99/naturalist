package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageQuery.LifeStageEntityQuery;

import java.util.Set;

class LifeStageEntityQueryImpl
        extends AbstractEntityQuery<
                        LifeStageName,
                        LifeStage,
                        LifeStageCollection,
                        LifeStageRepository.LifeStageEntityRepository>
        implements LifeStageEntityQuery {

    LifeStageEntityQueryImpl(LifeStageRepository.LifeStageEntityRepository repository) {
        super(repository);
    }

    @Override
    public LifeStageCollection findByNameSet(Set<LifeStageName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return LifeStageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public LifeStageCollection forSpeciesName(InsectSpeciesName speciesName) {
        observer().arguments("forSpeciesName", i -> i.entityName(speciesName, "speciesName"))
                .throwWhenInvalid();
        return LifeStageCollection.of(repository().getBySpeciesName(speciesName));
    }
}