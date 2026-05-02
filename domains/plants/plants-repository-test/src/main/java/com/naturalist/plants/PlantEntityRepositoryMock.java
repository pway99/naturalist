package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
public class PlantEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantName, Plant, PlantTestEntitySource>
        implements PlantRepository.PlantEntityRepository {

    protected PlantEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantName> getAllPlantNames() {
        return testEntitySource().entityStream()
                .map(Plant::name)
                .toList();
    }
}
