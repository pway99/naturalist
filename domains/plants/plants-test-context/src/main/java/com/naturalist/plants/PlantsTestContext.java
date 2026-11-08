package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.heritage.SeedLineageQuery;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import com.naturalist.test.context.ObjectGraph;

/**
 * Pre-wired, in-memory read surface for the plants bounded context, for other modules' tests and
 * external adapters. A facade over one {@link ObjectGraph} per {@link #create} call: the wiring lives
 * once, in plants-core's {@code PlantsTestContextInternal} and its sub-context siblings (cultivar,
 * heritage, management, phytochemistry, catalog) plus the graph's reflection over the
 * {@code @DomainService} and {@code @MockDomainService} classes. Each accessor returns what the graph
 * builds, already traced.
 *
 * <p><b>Read seam only.</b> Write-side test-data manipulation goes through the supplied
 * {@link NaturalistDatabase} and the domain's {@code *TestEntitySource} classes; do not grow write
 * methods on this class.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap a
 * {@code NaturalistTestExtension} (from {@code framework-test}) alongside this context.
 */
public class PlantsTestContext {

    private final ObjectGraph graph;

    private PlantsTestContext(NaturalistDatabase db) {
        this.graph = ObjectGraph.create(db);
    }

    public static PlantsTestContext create(NaturalistDatabase db) {
        return new PlantsTestContext(db);
    }

    public PlantQuery plantQuery() {
        return graph.get(PlantQuery.class);
    }

    public CultivarQuery cultivarQuery() {
        return graph.get(CultivarQuery.class);
    }

    public SeedLineageQuery seedLineageQuery() {
        return graph.get(SeedLineageQuery.class);
    }

    public PlantProgramQuery plantProgramQuery() {
        return graph.get(PlantProgramQuery.class);
    }

    public PhytochemicalConstituentQuery phytochemicalConstituentQuery() {
        return graph.get(PhytochemicalConstituentQuery.class);
    }
}
