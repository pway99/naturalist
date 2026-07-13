package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;

import java.util.List;
import java.util.Set;

@DomainService
class FieldObservationRepositoryMock
        extends AbstractTestEntityRepository<FieldObservationId, FieldObservation, FieldObservationTestEntitySource>
        implements InsectRepository.FieldObservationRepository {

    FieldObservationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<FieldObservation> getByNaturalist(NaturalistName observedBy) {
        observer().arguments("getByNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy))
                .toList();
    }

    @Override
    public List<FieldObservation> getByNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("getByNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy) && subjects.contains(o.subject()))
                .toList();
    }
}
