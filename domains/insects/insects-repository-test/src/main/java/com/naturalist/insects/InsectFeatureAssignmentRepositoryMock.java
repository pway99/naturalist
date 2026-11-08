package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

import java.util.List;
import java.util.Set;

@MockDomainService
class InsectFeatureAssignmentRepositoryMock
        extends AbstractTestEntityRepository<InsectFeatureAssignmentId,
                OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>,
                InsectFeatureAssignmentTestEntitySource>
        implements InsectRepository.FeatureAssignmentRepository {

    InsectFeatureAssignmentRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByRankName(InsectRankName rankName) {
        MethodObserver getByRankName = observer().forMethod("getByRankName");
        getByRankName.arguments(i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> a.rankName().equals(rankName))
                .toList();
        getByRankName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByRankNames(Set<InsectRankName> rankNames) {
        MethodObserver getByRankNames = observer().forMethod("getByRankNames");
        getByRankNames.arguments(i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> rankNames.contains(a.rankName()))
                .toList();
        getByRankNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByFeatureId(InsectFeatureId featureId) {
        MethodObserver getByFeatureId = observer().forMethod("getByFeatureId");
        getByFeatureId.arguments(i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> a.featureId().equals(featureId))
                .toList();
        getByFeatureId.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
