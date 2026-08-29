package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismObservation;

import java.util.List;
import java.util.Set;

@MockDomainService
class PlantObservationRepositoryMock
        extends AbstractTestEntityRepository<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>, PlantObservationTestEntitySource>
        implements PlantRepository.ObservationRepository {

    PlantObservationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalist(NaturalistName observedBy) {
        observer().arguments("getByNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> observedBy.equals(o.observedBy()))
                .toList();
    }

    @Override
    public List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalistAndSubjects(NaturalistName observedBy, Set<PlantRankName> subjects) {
        observer().arguments("getByNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> observedBy.equals(o.observedBy()) && subjects.contains(o.subject()))
                .toList();
    }
}
