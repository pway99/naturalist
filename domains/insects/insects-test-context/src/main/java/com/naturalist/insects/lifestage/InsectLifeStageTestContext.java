package com.naturalist.insects.lifestage;

import com.naturalist.test.context.TestGraph;
import com.naturalist.data.NaturalistDatabase;

/**
 * External test context of the insect life stage sub-context: a facade over a fresh {@link TestGraph},
 * which builds {@code InsectLifeStageQueryImpl}, its {@code InsectLifeStageEntityQueryImpl} and the
 * {@code InsectLifeStageEntityRepositoryMock} by reflection under insects-core's
 * {@code InsectLifeStageTestContextInternal}. The wiring lives there once.
 */
public final class InsectLifeStageTestContext {

    private InsectLifeStageTestContext() {
    }

    public static InsectLifeStageQuery createQuery(NaturalistDatabase db) {
        return TestGraph.create(db).get(InsectLifeStageQuery.class);
    }
}
