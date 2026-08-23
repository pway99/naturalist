package com.naturalist.insects;

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
        // Every node lands in-console (/insects/clades), Eukaryota (the shared root) included,
        // so the row never strands the naturalist in the shared tree-of-life browser.
        assertThat(html).contains("href=\"/insects/clades/eukaryota\"");
        assertThat(html).contains("href=\"/insects/clades/insecta\"");
        assertThat(html).doesNotContain("not yet placed");
        // Hover dropdowns present each node's narrower clades — Insecta → Holometabola …
        assertThat(html).contains("clade-menu");
        assertThat(html).contains("href=\"/insects/clades/holometabola\"");
        // … and Eukaryota's dropdown reaches Plantae, crossing into the plants console.
        assertThat(html).contains("href=\"/plants/clades/plantae\"");
        // Never a link into the shared /clades browser.
        assertThat(html).doesNotContain("href=\"/clades/");
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
