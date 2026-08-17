package com.naturalist.insects.console;

import com.naturalist.library.CladeStep;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsCladeTrailTemplateTest {

    @Test
    void rendersCladeNodesLinkingIntoClades() {
        CladeTrail trail = new CladeTrail(List.of(
                new CladeStep("eukaryota", "Eukaryota", Optional.empty()),
                new CladeStep("insecta", "Insecta", Optional.empty())), null);
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/cladeTrail.jte", Map.of("trail", trail), output);

        String html = output.toString();
        assertThat(html).contains("class=\"clade-trail-label\"");
        assertThat(html).contains("Tree of life");
        assertThat(html).contains("href=\"/clades/eukaryota\"");
        assertThat(html).contains("href=\"/clades/insecta\"");
        assertThat(html).doesNotContain("not yet placed");
        // Each node carries a hover dropdown of its narrower (child) clades — the
        // Insecta node reaches Holometabola beyond the trail path itself.
        assertThat(html).contains("clade-menu");
        assertThat(html).contains("href=\"/clades/holometabola\"");
        // Crossing kingdoms lands in the other console: Plantae, under Eukaryota, links
        // into the plants catalog rather than the shared tree-of-life browser.
        assertThat(html).contains("href=\"/plants/clades/plantae\"");
    }

    @Test
    void rendersGhostNodeForUnmappedEntity() {
        CladeTrail trail = new CladeTrail(List.of(
                new CladeStep("insecta", "Insecta", Optional.empty())), "Coleoptera");
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/cladeTrail.jte", Map.of("trail", trail), output);

        String html = output.toString();
        assertThat(html).contains("Coleoptera — not yet placed");
        assertThat(html).contains("href=\"/concepts/placing-clades\"");
    }
}
