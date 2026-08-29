package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class InsectFeatureRepositoryMock
        extends AbstractTestEntityRepository<InsectFeatureId, InsectFeature, InsectFeatureTestEntitySource>
        implements InsectRepository.FeatureRepository {

    InsectFeatureRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
