package com.naturalist.test.context;

/**
 * A package's test context: it guides how {@link TestGraph} builds the classes of its own Java package
 * ({@code getClass().getPackageName()}) and is where a test hands over its fakes. Register a concrete
 * subclass, which needs a public no-arg constructor, in
 * {@code META-INF/services/com.naturalist.test.context.TestContext}; one graph holds one instance of each.
 *
 * <p>A context with nothing to decide declares no provisions: the graph builds the package's
 * {@code @DomainService} and {@code @MockDomainService} classes by reflection.
 */
public abstract class TestContext {
    private TestGraph graph;

    protected TestContext() {
    }

    /**
     * Declares what this context builds by hand: a type reflection cannot build, the choice between two
     * candidates, or an outside-world port. Called once when the graph is created.
     */
    protected void provisions(Provisions p) {
    }

    /**
     * Replaces {@code type} everywhere in this context's graph. Hand the fake over before the first
     * {@code get} that would build the type.
     */
    public final <T> void fake(Class<T> type, T fake) {
        if (graph == null) {
            throw new TestGraphException(getClass().getSimpleName() + " belongs to no graph; take it from NaturalistTestExtension.context");
        }
        graph.registerFake(this, type, fake);
    }

    final void attach(TestGraph graph) {
        this.graph = graph;
    }
}
