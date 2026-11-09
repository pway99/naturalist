package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * Pre-wired, in-memory read surface for the soil bounded context, exposing the {@link SoilProfileQuery}
 * (assembled profile) and the {@link SoilProfileInfoQuery} (profile enumeration). A facade over one
 * {@link TestGraph} per {@link #create} call: the wiring lives once, in soil-core's
 * {@code SoilsTestContextInternal} and {@code SoilObservationTestContextInternal} plus the graph's reflection
 * over the {@code @DomainService} and {@code @MockDomainService} classes. Each accessor returns what the graph
 * builds, already traced.
 *
 * <p>Test data goes into the supplied database through the soil {@code *TestEntitySource} classes; do not
 * grow write methods on this class.
 */
public class SoilTestContext {

    private final TestGraph graph;

    private SoilTestContext(NaturalistDatabase db) {
        this.graph = TestGraph.create(db);
    }

    public static SoilTestContext create(NaturalistDatabase db) {
        return new SoilTestContext(db);
    }

    public SoilProfileQuery soilProfileQuery() {
        return graph.get(SoilProfileQuery.class);
    }

    public SoilProfileInfoQuery soilProfileInfoQuery() {
        return graph.get(SoilProfileInfoQuery.class);
    }
}
