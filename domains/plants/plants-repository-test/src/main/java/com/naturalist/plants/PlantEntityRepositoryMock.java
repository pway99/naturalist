package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Observer;

public class PlantEntityRepositoryMock extends AbstractTestEntityRepository<PlantId, PlantName, Plant, PlantTestEntitySource>
    implements PlantRepository.PlantEntityRepository {
    private static final Observer observer = Observer.forClass(PlantEntityRepositoryMock.class);

    protected PlantEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Observer observer() {
        return observer;
    }
}
