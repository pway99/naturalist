package com.naturalist.plants;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Magnoliids;
import com.naturalist.library.CladeStep;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/cladeTrail.jte} — the phylogenetic tree-of-life row,
 * now fed the shared library read model's {@link CladeStep}s (root→subject).
 */
class PlantsCladeTrailTemplateTest {

    /**
     * Real lineage as {@link CladeStep}s. {@code rank} is left {@link Optional#empty()}
     * here — unlike the runtime {@code CladeViewFactory}, which populates it — because
     * the template never reads it; only slug and displayName drive the rendered row.
     */
    private static List<CladeStep> steps(Clade tip) {
        return CladeTraversal.ancestry(tip).reversed().stream()
                .map(c -> new CladeStep(c.slug(), c.displayName(), Optional.empty()))
                .toList();
    }

    // Piperales → magnoliids; the trail runs root→subject, Eukaryota → … → Magnoliids.
    private static final List<CladeStep> MAGNOLIID_TRAIL = steps(new Magnoliids());

    @Test
    void trailRunsRootToSubjectEndingAtMagnoliids() {
        assertThat(MAGNOLIID_TRAIL).extracting(CladeStep::cladeSlug)
                .startsWith("eukaryota", "plantae")
                .endsWith("magnoliids");
    }

    @Test
    void rendersInConsoleLineageWithHoverDropdownsAndTheEukaryotaCrossover() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", MAGNOLIID_TRAIL), output);

        String html = output.toString();
        assertThat(html).contains("Tree of life");
        // Every node lands on an in-console clade page — even Eukaryota, the shared root.
        assertThat(html).contains("href=\"/plants/clades/eukaryota\"");
        assertThat(html).contains("href=\"/plants/clades/plantae\"");
        assertThat(html).contains("href=\"/plants/clades/magnoliids\"");
        // Hover dropdowns present each node's narrower clades — Angiosperms → Monocots …
        assertThat(html).contains("clade-menu");
        assertThat(html).contains("href=\"/plants/clades/monocots\"");
        // … and Eukaryota's dropdown reaches Animalia, crossing into the insects console.
        assertThat(html).contains("href=\"/insects/clades/animalia\"");
        // Never a link into the shared /clades tree-of-life browser.
        assertThat(html).doesNotContain("href=\"/clades/");
    }

    @Test
    void surfacesTheMissingClassAsALearningLink() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", MAGNOLIID_TRAIL), output);

        // Plants carry no Class rank (design D1). Rather than silently skipping it,
        // the trail links a learner to the four-level "What is a class?" concept, so
        // the supra-ordinal gap becomes a teaching moment rather than a blank.
        assertThat(output.toString()).contains("href=\"/concepts/class\"");
    }

    @Test
    void emptyTrailRendersNothing() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", List.<CladeStep>of()), output);

        assertThat(output.toString()).doesNotContain("Tree of life");
    }
}
