package com.naturalist.soil;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

/**
 * In-memory {@link SoilProfileInfoRepository} backed by {@link SoilProfileInfoTestEntitySource}.
 */
@MockDomainService
class SoilProfileInfoEntityRepositoryMock
        extends AbstractTestEntityRepository<SoilProfileName, SoilProfileInfo, SoilProfileInfoTestEntitySource>
        implements SoilProfileInfoRepository {

    protected SoilProfileInfoEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
