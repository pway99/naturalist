package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.plants.PlantEntityCollections.PlantObservationCollection;

import java.util.Set;

@DomainService
class PlantObservationQueryImpl
        extends AbstractEntityQuery<
        PlantObservationId,
        OrganismObservation<PlantObservationId, PlantRankName>,
        PlantObservationCollection,
        PlantRepository.PlantObservationRepository>
        implements PlantQuery.PlantObservationQuery {

    PlantObservationQueryImpl(PlantRepository.PlantObservationRepository repository) {
        super(repository);
    }

    @Override
    public PlantObservationCollection findByNameSet(Set<PlantObservationId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantObservationCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public PlantObservationCollection forNaturalist(NaturalistName observedBy) {
        observer().arguments("forNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return PlantObservationCollection.of(repository().getByNaturalist(observedBy));
    }

    @Override
    public PlantObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<PlantRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return PlantObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
