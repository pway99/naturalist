package com.naturalist.chemistry.product;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.ObjectGraph;

/**
 * The product sub-context's query over its own {@link ObjectGraph}. The wiring lives in chemistry-core's
 * {@code ProductTestContextInternal} and the graph's reflection over the {@code @DomainService} and
 * {@code @MockDomainService} classes; the query comes back already traced.
 */
public final class ProductTestContext {

    private ProductTestContext() {
    }

    public static ProductQuery createQuery(NaturalistDatabase db) {
        return ObjectGraph.create(db).get(ProductQuery.class);
    }
}
