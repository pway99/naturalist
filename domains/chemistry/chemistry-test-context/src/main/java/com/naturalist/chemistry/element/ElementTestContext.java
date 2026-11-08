package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.ObjectGraph;

/**
 * The element sub-context's query over its own {@link ObjectGraph}. The wiring lives in chemistry-core's
 * {@code ElementTestContextInternal} and the graph's reflection over the {@code @DomainService} and
 * {@code @MockDomainService} classes; the query comes back already traced.
 */
public final class ElementTestContext {

    private ElementTestContext() {
    }

    public static ElementQuery createQuery(NaturalistDatabase db) {
        return ObjectGraph.create(db).get(ElementQuery.class);
    }
}
