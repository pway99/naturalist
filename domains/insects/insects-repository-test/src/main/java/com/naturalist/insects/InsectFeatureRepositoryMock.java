package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class InsectFeatureRepositoryMock
        extends AbstractTestEntityRepository<InsectFeatureId, InsectFeature, InsectFeatureTestEntitySource>
        implements InsectRepository.FeatureRepository {

    InsectFeatureRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
