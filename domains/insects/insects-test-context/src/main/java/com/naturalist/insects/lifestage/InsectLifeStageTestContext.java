package com.naturalist.insects.lifestage;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the insect life stage sub-context. Lives in
 * {@code com.naturalist.insects.lifestage} so it can instantiate the
 * package-private {@link InsectLifeStageQueryImpl} and
 * {@link LifeStageEntityQueryImpl} adapters alongside the protected-constructor
 * {@link LifeStageEntityRepositoryMock}.
 */
public final class InsectLifeStageTestContext {

    private InsectLifeStageTestContext() {}

    public static InsectLifeStageQuery createQuery(NaturalistDatabase db) {
        LifeStageEntityRepositoryMock repository = new LifeStageEntityRepositoryMock(db);
        InsectLifeStageQuery.LifeStageEntityQuery entityQuery = new LifeStageEntityQueryImpl(repository);
        return new InsectLifeStageQueryImpl(entityQuery);
    }
}
