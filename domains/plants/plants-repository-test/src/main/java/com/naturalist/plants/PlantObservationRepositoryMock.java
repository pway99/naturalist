package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
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
        MethodObserver getByNaturalist = observer().forMethod("getByNaturalist");
        getByNaturalist.arguments(i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(o -> observedBy.equals(o.observedBy()))
                .toList();
        getByNaturalist.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalistAndSubjects(NaturalistName observedBy, Set<PlantRankName> subjects) {
        MethodObserver getByNaturalistAndSubjects = observer().forMethod("getByNaturalistAndSubjects");
        getByNaturalistAndSubjects.arguments(i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(o -> observedBy.equals(o.observedBy()) && subjects.contains(o.subject()))
                .toList();
        getByNaturalistAndSubjects.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
