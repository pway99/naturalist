package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

import java.util.List;
import java.util.Set;

class PlantFeatureAssignmentRepositoryMock
        extends AbstractTestEntityRepository<PlantFeatureAssignmentId,
                OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>,
                PlantFeatureAssignmentTestEntitySource>
        implements PlantRepository.FeatureAssignmentRepository {

    PlantFeatureAssignmentRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> getByRankName(PlantRankName rankName) {
        observer().arguments("getByRankName",
                        i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.rankName().equals(rankName))
                .toList();
    }

    @Override
    public List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> getByRankNames(Set<PlantRankName> rankNames) {
        observer().arguments("getByRankNames",
                        i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> rankNames.contains(a.rankName()))
                .toList();
    }

    @Override
    public List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> getByFeatureId(PlantFeatureId featureId) {
        observer().arguments("getByFeatureId",
                        i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.featureId().equals(featureId))
                .toList();
    }
}
