package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;

import java.util.List;
import java.util.Set;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observation.OrganismObservation;

@MockDomainService
class InsectObservationRepositoryMock
        extends AbstractTestEntityRepository<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, InsectObservationTestEntitySource>
        implements InsectRepository.ObservationRepository {

    InsectObservationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalist(NaturalistName observedBy) {
        MethodObserver getByNaturalist = observer().forMethod("getByNaturalist");
        getByNaturalist.arguments(i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy))
                .toList();
        getByNaturalist.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        MethodObserver getByNaturalistAndSubjects = observer().forMethod("getByNaturalistAndSubjects");
        getByNaturalistAndSubjects.arguments(i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy) && subjects.contains(o.subject()))
                .toList();
        getByNaturalistAndSubjects.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
