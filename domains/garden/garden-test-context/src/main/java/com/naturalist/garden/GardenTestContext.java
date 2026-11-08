package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.ObjectGraph;

/**
 * Pre-wired, in-memory read surface for the garden bounded context, exposing the {@link PlantedZoneQuery}
 * (a bed and what is in it) and the {@link PlantingQuery}. A facade over one {@link ObjectGraph} per
 * {@link #create} call: the wiring lives once, in garden-core's {@code GardenTestContextInternal} plus the
 * graph's reflection over the {@code @DomainService} and {@code @MockDomainService} classes. Each accessor
 * returns what the graph builds, already traced.
 *
 * <p>Test data goes into the supplied database through the garden {@code *TestEntitySource} classes; do not
 * grow write methods on this class.
 */
public class GardenTestContext {

    private final ObjectGraph graph;

    private GardenTestContext(NaturalistDatabase db) {
        this.graph = ObjectGraph.create(db);
    }

    public static GardenTestContext create(NaturalistDatabase db) {
        return new GardenTestContext(db);
    }

    public PlantingQuery plantingQuery() {
        return graph.get(PlantingQuery.class);
    }

    public PlantedZoneQuery plantedZoneQuery() {
        return graph.get(PlantedZoneQuery.class);
    }
}
