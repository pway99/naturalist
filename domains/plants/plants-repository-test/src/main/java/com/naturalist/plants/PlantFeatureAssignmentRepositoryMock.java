package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;
import java.util.Set;

@DomainService
class PlantFeatureAssignmentRepositoryMock
        extends AbstractTestEntityRepository<PlantFeatureAssignmentId, PlantFeatureAssignment, PlantFeatureAssignmentTestEntitySource>
        implements PlantRepository.FeatureAssignmentRepository {

    PlantFeatureAssignmentRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantFeatureAssignment> getByRankName(PlantRankName rankName) {
        observer().arguments("getByRankName",
                        i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.rankName().equals(rankName))
                .toList();
    }

    @Override
    public List<PlantFeatureAssignment> getByRankNames(Set<PlantRankName> rankNames) {
        observer().arguments("getByRankNames",
                        i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> rankNames.contains(a.rankName()))
                .toList();
    }

    @Override
    public List<PlantFeatureAssignment> getByFeatureId(PlantFeatureId featureId) {
        observer().arguments("getByFeatureId",
                        i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.featureId().equals(featureId))
                .toList();
    }
}
