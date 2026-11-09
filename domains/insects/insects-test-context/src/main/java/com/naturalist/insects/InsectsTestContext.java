package com.naturalist.insects;

import com.naturalist.test.context.TestGraph;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.observation.ImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.vision.VisionService;

/**
 * Pre-wired, in-memory read and write surface for the insects bounded context, for other domains'
 * tests and external adapters. A facade over one {@link TestGraph} per {@code create} call: the wiring
 * lives once, in insects-core's {@link InsectsTestContextInternal} (and library's and life stage's
 * internal contexts) plus the graph's reflection over the {@code @DomainService} and
 * {@code @MockDomainService} classes. Each accessor returns what the graph builds, already traced.
 *
 * <p>Each {@code create} overload hands {@link InsectsTestContextInternal} only the outside-world ports
 * its caller passed ({@link ImageStore}, {@link Resilience}, {@link IdentificationBudget},
 * {@link VisionService}, {@link ExternalAuthority}) as fakes, which replace those types everywhere in the
 * graph; every port not passed is the internal context's default, which lives there alone.
 *
 * <p>Bulk test-data seeding still goes through the supplied {@link NaturalistDatabase} and the domain's
 * {@code *TestEntitySource} classes — the command surface exists for exercising the public write port
 * end-to-end, not for fixture loading.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap a
 * {@code NaturalistTestExtension} (from {@code framework-test}) alongside this context.
 */
public class InsectsTestContext {
    private final TestGraph graph;

    private InsectsTestContext(TestGraph graph) {
        this.graph = graph;
    }

    /** Every outside-world port is {@link InsectsTestContextInternal}'s default. */
    public static InsectsTestContext create(NaturalistDatabase db) {
        return new InsectsTestContext(TestGraph.create(db));
    }

    /** Hands over {@code imageStore}; every other outside-world port is the internal context's default. */
    public static InsectsTestContext create(NaturalistDatabase db, ImageStore imageStore) {
        TestGraph graph = TestGraph.create(db);
        graph.context(InsectsTestContextInternal.class).fake(ImageStore.class, imageStore);
        return new InsectsTestContext(graph);
    }

    /**
     * Full overload: the identification port ({@code insectCommand().identification()}) runs
     * behind the given rate-limiting {@link Resilience}, {@link IdentificationBudget},
     * {@link VisionService} and {@link ExternalAuthority}.
     */
    public static InsectsTestContext create(NaturalistDatabase db, ImageStore imageStore, Resilience resilience,
                                            IdentificationBudget budget, VisionService visionService,
                                            ExternalAuthority authority) {
        TestGraph graph = TestGraph.create(db);
        InsectsTestContextInternal context = graph.context(InsectsTestContextInternal.class);
        context.fake(ImageStore.class, imageStore);
        context.fake(Resilience.class, resilience);
        context.fake(IdentificationBudget.class, budget);
        context.fake(VisionService.class, visionService);
        context.fake(ExternalAuthority.class, authority);
        return new InsectsTestContext(graph);
    }

    /** A no-op {@link ExternalAuthority}, for callers of the full overload that never reach enrichment. */
    public static ExternalAuthority noOpAuthority() {
        return new NoOpExternalAuthority();
    }

    public InsectQuery insectQuery() {
        return graph.get(InsectQuery.class);
    }

    public InsectViewQuery insectViewQuery() {
        return graph.get(InsectViewQuery.class);
    }

    public InsectCommand insectCommand() {
        return graph.get(InsectCommand.class);
    }

    public InsectEntityCommands entityCommands() {
        return graph.get(InsectEntityCommands.class);
    }

    public InsectLifeStageQuery insectLifeStageQuery() {
        return graph.get(InsectLifeStageQuery.class);
    }

    public InsectCatalogIdentificationTransaction catalogIdentificationTransaction() {
        return graph.get(InsectCatalogIdentificationTransaction.class);
    }

    public InsectAddPhotoTransaction addPhotoTransaction() {
        return graph.get(InsectAddPhotoTransaction.class);
    }
}
