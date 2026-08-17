package com.naturalist.plants.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
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

    // Piperales → magnoliids; the plant order's trail opens at Plantae (the shared
    // Eukaryota root above it is omitted to keep the row within the plant world).
    private static final List<Clade> MAGNOLIID_TRAIL =
            CladeTraversal.ancestry(new Magnoliids()).reversed().stream()
                    .filter(clade -> !(clade instanceof Eukaryota))
                    .toList();

    @Test
    void rendersLineageFromPlantaeToSubjectLinkingIntoClades() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", MAGNOLIID_TRAIL), output);

        String html = output.toString();
        assertThat(html).contains("Tree of life");
        assertThat(html).contains("href=\"/clades/plantae\"");
        assertThat(html).contains("href=\"/clades/angiosperms\"");
        assertThat(html).contains("href=\"/clades/magnoliids\"");
        // The row opens at the plant kingdom, not the shared universal root.
        assertThat(html).doesNotContain("href=\"/clades/eukaryota\"");
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
