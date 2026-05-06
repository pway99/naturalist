package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
public class PlantGenusEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantGenusName, PlantGenus, PlantGenusTestEntitySource>
        implements PlantRepository.PlantGenusEntityRepository {

    protected PlantGenusEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
