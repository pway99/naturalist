package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.FieldObservationCollection;
import com.naturalist.naturalist.NaturalistName;

import java.util.Set;

class FieldObservationQueryImpl
        extends AbstractEntityQuery<
        FieldObservationId,
        FieldObservation,
        FieldObservationCollection,
        InsectRepository.FieldObservationRepository>
        implements InsectQuery.FieldObservationQuery {

    FieldObservationQueryImpl(InsectRepository.FieldObservationRepository repository) {
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
    public FieldObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
