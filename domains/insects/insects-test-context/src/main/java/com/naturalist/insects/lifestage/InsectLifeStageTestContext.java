package com.naturalist.insects.lifestage;

import com.naturalist.test.query.nofanout.SelectTrace;
import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the insect life stage sub-context. Lives in
 * {@code com.naturalist.insects.lifestage} so it can instantiate the
 * package-private {@link InsectLifeStageQueryImpl} and
 * {@link InsectLifeStageEntityQueryImpl} adapters alongside the protected-constructor
 * {@link InsectLifeStageEntityRepositoryMock}.
 */
public final class InsectLifeStageTestContext {

    private InsectLifeStageTestContext() {
    }

    public static InsectLifeStageQuery createQuery(NaturalistDatabase db) {
        InsectLifeStageEntityRepositoryMock repository = new InsectLifeStageEntityRepositoryMock(db);
        InsectLifeStageQuery.LifeStageEntityQuery entityQuery = new InsectLifeStageEntityQueryImpl(repository);
        return SelectTrace.traced(InsectLifeStageQuery.class, new InsectLifeStageQueryImpl(entityQuery));
    }
}
