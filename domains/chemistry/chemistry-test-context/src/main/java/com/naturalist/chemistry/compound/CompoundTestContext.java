package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.ObjectGraph;

/**
 * The compound sub-context's query over its own {@link ObjectGraph}. The wiring lives in chemistry-core's
 * {@code CompoundTestContextInternal} and the graph's reflection over the {@code @DomainService} and
 * {@code @MockDomainService} classes; the query comes back already traced.
 */
public final class CompoundTestContext {

    private CompoundTestContext() {
    }

    public static CompoundQuery createQuery(NaturalistDatabase db) {
        return ObjectGraph.create(db).get(CompoundQuery.class);
    }
}
