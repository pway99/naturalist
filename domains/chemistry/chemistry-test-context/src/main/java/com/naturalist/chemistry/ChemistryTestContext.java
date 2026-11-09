package com.naturalist.chemistry;

import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.element.ElementQuery;
import com.naturalist.chemistry.product.ProductQuery;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * Pre-wired, in-memory read surface for the chemistry bounded context, for other modules' tests and
 * console slices. A facade over one {@link TestGraph} per {@link #create} call: the wiring lives once,
 * in chemistry-core's {@code *TestContextInternal} classes (compound, element, product, catalog) plus the
 * graph's reflection over the {@code @DomainService} and {@code @MockDomainService} classes. Each accessor
 * returns what the graph builds, already traced.
 *
 * <p><b>Read seam only.</b> Write-side test-data manipulation goes through
 * the supplied {@link NaturalistDatabase} and the domain's
 * {@code *TestEntitySource} classes; do not grow write methods on this class.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap
 * a {@code NaturalistTestExtension} (from {@code framework-test})
 * alongside this context.
 */
public class ChemistryTestContext {

    private final TestGraph graph;

    private ChemistryTestContext(NaturalistDatabase db) {
        this.graph = TestGraph.create(db);
    }

    public static ChemistryTestContext create(NaturalistDatabase db) {
        return new ChemistryTestContext(db);
    }

    public CompoundQuery compoundQuery() {
        return graph.get(CompoundQuery.class);
    }

    public ProductQuery productQuery() {
        return graph.get(ProductQuery.class);
    }

    public ElementQuery elementQuery() {
        return graph.get(ElementQuery.class);
    }
}
