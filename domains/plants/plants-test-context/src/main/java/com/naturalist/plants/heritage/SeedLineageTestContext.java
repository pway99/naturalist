package com.naturalist.plants.heritage;

import com.naturalist.data.NaturalistDatabase;

public final class SeedLineageTestContext {

    private SeedLineageTestContext() {
    }

    public static SeedLineageQuery createQuery(NaturalistDatabase db) {
        SeedLineageRepository.SeedLineageEntityRepository repository = new SeedLineageEntityRepositoryMock(db);
        SeedLineageQuery.SeedLineageEntityQuery entityQuery = new SeedLineageEntityQueryImpl(repository);
        return new SeedLineageQueryImpl(entityQuery);
    }
}
