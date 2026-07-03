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
