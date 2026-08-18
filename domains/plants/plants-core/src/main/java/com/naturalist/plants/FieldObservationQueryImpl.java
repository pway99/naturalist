package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.plants.PlantEntityCollections.FieldObservationCollection;

import java.util.Set;

@DomainService
class FieldObservationQueryImpl
        extends AbstractEntityQuery<
        FieldObservationId,
        FieldObservation,
        FieldObservationCollection,
        PlantRepository.FieldObservationRepository>
        implements PlantQuery.FieldObservationQuery {

    FieldObservationQueryImpl(PlantRepository.FieldObservationRepository repository) {
        super(repository);
    }

    @Override
    public FieldObservationCollection findByNameSet(Set<FieldObservationId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public FieldObservationCollection forNaturalist(NaturalistName observedBy) {
        observer().arguments("forNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByNaturalist(observedBy));
    }

    @Override
    public FieldObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<PlantRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
