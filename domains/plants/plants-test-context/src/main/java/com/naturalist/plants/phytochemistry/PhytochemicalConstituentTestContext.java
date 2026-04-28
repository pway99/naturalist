package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;

public final class PhytochemicalConstituentTestContext {

    private PhytochemicalConstituentTestContext() {}

    public static PhytochemicalConstituentQuery createQuery(NaturalistDatabase db) {
        PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository repository =
                new PhytochemicalConstituentEntityRepositoryMock(db);
        PhytochemicalConstituentQuery.PhytochemicalConstituentEntityQuery entityQuery =
                new PhytochemicalConstituentEntityQueryImpl(repository);
        return new PhytochemicalConstituentQueryImpl(entityQuery);
    }
}
