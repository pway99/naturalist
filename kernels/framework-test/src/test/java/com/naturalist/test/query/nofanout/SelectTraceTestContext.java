package com.naturalist.test.query.nofanout;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.Provisions;
import com.naturalist.test.context.TestContext;

/**
 * The test context of the SelectTrace fixtures' package, so {@code SelectTraceTest} can take a graph-built
 * {@code SelectTrace.Traced} object from {@code nte.get}. It provides the fixture repository the graph cannot
 * build (it is no {@code @MockDomainService}); {@link GraphFooLookup} is built by reflection.
 */
public final class SelectTraceTestContext extends TestContext {
    public SelectTraceTestContext() {
    }

    @Override
    protected void provisions(Provisions p) {
        p.provide(FooRepository.class, graph -> new FooRepository(graph.get(NaturalistDatabase.class)));
    }
}
