package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

import java.time.Clock;

/**
 * Pre-wired, in-memory usage bounded context for other modules' tests and console slices. A facade over one
 * {@link TestGraph} per {@link #create} call: the wiring lives once, in usage-core's
 * {@code UsagesTestContextInternal} plus the graph's reflection over the {@code @DomainService} and
 * {@code @MockDomainService} classes. Each accessor returns what the graph builds, already traced.
 *
 * <p>The production {@link UsageQuery} / {@link UsageCommand} / {@link IdentificationBudget} share one
 * {@link NaturalistDatabase}, so a slice test can reserve budget through {@link #identificationBudget()} and
 * read the result back through {@link #usageQuery()}, the same shared-state coupling the production trio has.
 * {@code warningPercent} and {@code clock} are supplied by the caller (a fixed clock keeps window-based
 * snapshots deterministic); the clock is the graph's fake for the outside-world {@link Clock} port.
 *
 * <p>Test data goes into the supplied database through the usage {@code *TestEntitySource} classes; do not
 * grow write methods on this class.
 */
public class UsagesTestContext {

    private final TestGraph graph;

    private UsagesTestContext(NaturalistDatabase db, int warningPercent, Clock clock) {
        this.graph = TestGraph.create(db);
        UsagesTestContextInternal context = graph.context(UsagesTestContextInternal.class);
        context.warningPercent(warningPercent);
        context.fake(Clock.class, clock);
    }

    public static UsagesTestContext create(NaturalistDatabase db, int warningPercent, Clock clock) {
        return new UsagesTestContext(db, warningPercent, clock);
    }

    public UsageQuery usageQuery() {
        return graph.get(UsageQuery.class);
    }

    public UsageCommand usageCommand() {
        return graph.get(UsageCommand.class);
    }

    public IdentificationBudget identificationBudget() {
        return graph.get(IdentificationBudget.class);
    }
}
