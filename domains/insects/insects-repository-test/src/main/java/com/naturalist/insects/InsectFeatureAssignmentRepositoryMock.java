package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
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
        observer().arguments("getByRankName",
                        i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.rankName().equals(rankName))
                .toList();
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByRankNames(Set<InsectRankName> rankNames) {
        observer().arguments("getByRankNames",
                        i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> rankNames.contains(a.rankName()))
                .toList();
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByFeatureId(InsectFeatureId featureId) {
        observer().arguments("getByFeatureId",
                        i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.featureId().equals(featureId))
                .toList();
    }
}
