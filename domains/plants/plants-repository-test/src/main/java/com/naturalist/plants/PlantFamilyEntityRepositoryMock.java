package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
public class PlantFamilyEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantFamilyName, PlantFamily, PlantFamilyTestEntitySource>
        implements PlantRepository.PlantFamilyEntityRepository {

    protected PlantFamilyEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantFamilyName> getAllFamilyNames() {
        return testEntitySource().entityStream()
                .map(PlantFamily::name)
                .toList();
    }
}
