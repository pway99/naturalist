package com.naturalist.plants.management;

import com.naturalist.data.NaturalistDatabase;

public final class PlantProgramTestContext {

    private PlantProgramTestContext() {}

    public static PlantProgramQuery createQuery(NaturalistDatabase db) {
        PlantProgramRepository.PlantProgramEntityRepository repository = new PlantProgramEntityRepositoryMock(db);
        PlantProgramQuery.PlantProgramEntityQuery entityQuery = new PlantProgramEntityQueryImpl(repository);
        return new PlantProgramQueryImpl(entityQuery);
    }
}
