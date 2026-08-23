package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.ObservationCollection;
import com.naturalist.naturalist.NaturalistName;

import java.util.Set;
import com.naturalist.observation.OrganismObservation;

@DomainService
class InsectObservationQueryImpl
        extends AbstractEntityQuery<
        InsectObservationId,
        OrganismObservation<InsectObservationId, InsectRankName>,
        ObservationCollection,
        InsectRepository.ObservationRepository>
        implements InsectQuery.ObservationQuery {

    InsectObservationQueryImpl(InsectRepository.ObservationRepository repository) {
        super(repository);
    }

    @Override
    public ObservationCollection findByNameSet(Set<InsectObservationId> names) {
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
    public ObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return ObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
