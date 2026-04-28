package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;

public final class CultivarTestContext {

    private CultivarTestContext() {}

    public static CultivarQuery createQuery(NaturalistDatabase db) {
        CultivarRepository.CultivarEntityRepository repository = new CultivarEntityRepositoryMock(db);
        CultivarQuery.CultivarEntityQuery entityQuery = new CultivarEntityQueryImpl(repository);
        return new CultivarQueryImpl(entityQuery);
    }
}
