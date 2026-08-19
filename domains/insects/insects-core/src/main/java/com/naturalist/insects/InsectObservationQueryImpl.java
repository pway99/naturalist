package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.InsectObservationCollection;
import com.naturalist.naturalist.NaturalistName;

import java.util.Set;
import com.naturalist.observation.OrganismObservation;

class InsectObservationQueryImpl
        extends AbstractEntityQuery<
        InsectObservationId,
        OrganismObservation<InsectObservationId, InsectRankName>,
        InsectObservationCollection,
        InsectRepository.InsectObservationRepository>
        implements InsectQuery.InsectObservationQuery {

    InsectObservationQueryImpl(InsectRepository.InsectObservationRepository repository) {
        super(repository);
    }

    @Override
    public InsectObservationCollection findByNameSet(Set<InsectObservationId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return InsectObservationCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public InsectObservationCollection forNaturalist(NaturalistName observedBy) {
        observer().arguments("forNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return InsectObservationCollection.of(repository().getByNaturalist(observedBy));
    }

    @Override
    public InsectObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return InsectObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
