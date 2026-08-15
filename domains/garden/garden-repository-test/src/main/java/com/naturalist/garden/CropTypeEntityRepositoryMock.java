package com.naturalist.garden;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * In-memory {@link CropTypeRepository} backed by {@link CropTypeTestEntitySource}.
 */
@DomainService
class CropTypeEntityRepositoryMock
        extends AbstractTestEntityRepository<CropTypeName, CropType, CropTypeTestEntitySource>
        implements CropTypeRepository {

    protected CropTypeEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
