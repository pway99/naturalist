package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;

import java.util.List;
import java.util.Set;
import com.naturalist.observation.OrganismObservation;

@DomainService
class OrganismObservationRepositoryMock
        extends AbstractTestEntityRepository<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, OrganismObservationTestEntitySource>
        implements InsectRepository.FieldObservationRepository {

    OrganismObservationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalist(NaturalistName observedBy) {
        observer().arguments("getByNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy))
                .toList();
    }

    @Override
    public List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("getByNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy) && subjects.contains(o.subject()))
                .toList();
    }
}
