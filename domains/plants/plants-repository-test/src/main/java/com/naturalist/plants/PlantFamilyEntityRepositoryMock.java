package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
public class PlantFamilyEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantFamilyName, PlantFamily, PlantFamilyTestEntitySource>
        implements PlantRepository.PlantFamilyEntityRepository {

    protected PlantFamilyEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
