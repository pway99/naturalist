package com.naturalist.soil;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * In-memory {@link SoilProfileInfoRepository} backed by {@link SoilProfileInfoTestEntitySource}.
 */
@DomainService
class SoilProfileInfoEntityRepositoryMock
        extends AbstractTestEntityRepository<SoilProfileName, SoilProfileInfo, SoilProfileInfoTestEntitySource>
        implements SoilProfileInfoRepository {

    protected SoilProfileInfoEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
