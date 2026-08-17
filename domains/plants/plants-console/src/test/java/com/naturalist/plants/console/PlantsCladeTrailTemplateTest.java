package com.naturalist.plants.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Magnoliids;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/cladeTrail.jte} — the phylogenetic tree-of-life row.
 */
class PlantsCladeTrailTemplateTest {

    // Piperales → magnoliids; the trail runs root→subject, Eukaryota → … → Magnoliids.
    private static final List<Clade> MAGNOLIID_TRAIL =
            CladeTraversal.ancestry(new Magnoliids()).reversed();

    @Test
    void rendersLineageWithInConsolePlantNodesAndTheEukaryotaCrossover() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", MAGNOLIID_TRAIL), output);

        String html = output.toString();
        assertThat(html).contains("Tree of life");
        // Plant clades drive plant queries — they link into the in-console clade pages.
        assertThat(html).contains("href=\"/plants/clades/plantae\"");
        assertThat(html).contains("href=\"/plants/clades/angiosperms\"");
        assertThat(html).contains("href=\"/plants/clades/magnoliids\"");
        // Eukaryota, the shared root, links to the cross-domain browser, and its dropdown
        // reaches Animalia — the two-way bridge to the insect side.
        assertThat(html).contains("href=\"/clades/eukaryota\"");
        assertThat(html).contains("href=\"/clades/animalia\"");
        // Each node's dropdown reaches its narrower clades (Angiosperms → Monocots).
        assertThat(html).contains("clade-menu");
        assertThat(html).contains("href=\"/plants/clades/monocots\"");
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
                "plants/cladeTrail.jte", Map.of("cladeTrail", List.of()), output);

        assertThat(output.toString()).doesNotContain("Tree of life");
    }
}
