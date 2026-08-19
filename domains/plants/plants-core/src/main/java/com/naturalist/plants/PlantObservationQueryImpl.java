package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.plants.PlantEntityCollections.ObservationCollection;

import java.util.Set;

@DomainService
class PlantObservationQueryImpl
        extends AbstractEntityQuery<
        PlantObservationId,
        OrganismObservation<PlantObservationId, PlantRankName>,
        ObservationCollection,
        PlantRepository.ObservationRepository>
        implements PlantQuery.ObservationQuery {

    PlantObservationQueryImpl(PlantRepository.ObservationRepository repository) {
        super(repository);
    }

    @Override
    public ObservationCollection findByNameSet(Set<PlantObservationId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return ObservationCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ObservationCollection forNaturalist(NaturalistName observedBy) {
        observer().arguments("forNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return ObservationCollection.of(repository().getByNaturalist(observedBy));
    }

    @Override
    public ObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<PlantRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return ObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
