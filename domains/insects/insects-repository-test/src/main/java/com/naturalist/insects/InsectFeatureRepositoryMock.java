package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectFeatureRepositoryMock
        extends AbstractTestEntityRepository<InsectFeatureId, InsectFeature, InsectFeatureTestEntitySource>
        implements InsectRepository.FeatureRepository {

    InsectFeatureRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
