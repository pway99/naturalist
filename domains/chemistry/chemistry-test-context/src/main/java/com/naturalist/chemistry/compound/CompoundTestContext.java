package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * The compound sub-context's query over its own {@link TestGraph}. The wiring lives in chemistry-core's
 * {@code CompoundTestContextInternal} and the graph's reflection over the {@code @DomainService} and
 * {@code @MockDomainService} classes; the query comes back already traced.
 */
public final class CompoundTestContext {

    private CompoundTestContext() {
    }

    public static CompoundQuery createQuery(NaturalistDatabase db) {
        return TestGraph.create(db).get(CompoundQuery.class);
    }
}
