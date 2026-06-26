package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class InsectFeatureAssignmentRepositoryMock
        extends AbstractTestEntityRepository<InsectFeatureAssignmentId, InsectFeatureAssignment, InsectFeatureAssignmentTestEntitySource>
        implements InsectRepository.FeatureAssignmentRepository {

    InsectFeatureAssignmentRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFeatureAssignment> getByRankName(InsectRankName rankName) {
        observer().arguments("getByRankName",
                        i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.rankName().equals(rankName))
                .toList();
    }

    @Override
    public List<InsectFeatureAssignment> getByFeatureId(InsectFeatureId featureId) {
        observer().arguments("getByFeatureId",
                        i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.featureId().equals(featureId))
                .toList();
    }
}
