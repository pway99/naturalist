package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Lepidoptera;
import com.naturalist.library.CladeStep;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code insects/clades/detail.jte} — a clade's page in the insect catalog:
 * insect taxa grouped by Linnaean rank (so the naturalist sees insect clades reach below
 * Order) plus narrower child clades, kept entirely in-console.
 */
class InsectsCladeDetailTemplateTest {

    @Test
    void cladeDetail_groupsTaxaByRankAndListsChildCladesInConsole() {
        CladeTrail trail = new CladeTrail(List.of(
                new CladeStep("eukaryota", "Eukaryota", Optional.empty()),
                new CladeStep("holometabola", "Holometabola", Optional.empty())), null);

        StringOutput output = new StringOutput();
        ConsoleSliceTemplates.create().render("insects/clades/detail.jte", Map.of(
                "clade", new Holometabola(),
                "orders", List.of(new CladeTaxonCard("/insects/orders/lepidoptera", "Lepidoptera")),
                "species", List.of(new CladeTaxonCard("/insects/battus-philenor", "Battus philenor")),
                "childClades", List.of(new Lepidoptera()),
                "cladeTrail", trail), output);

        String html = output.toString();
        // Taxa are grouped by rank, showing insect clades reach below Order (down to species).
        assertThat(html).contains("Orders in this clade");
        assertThat(html).contains("href=\"/insects/orders/lepidoptera\"");
        assertThat(html).contains("Species in this clade");
        assertThat(html).contains("Battus philenor");
        // Child clades drill deeper, staying in the insects console (never the shared browser).
        assertThat(html).contains("Clades within Holometabola");
        assertThat(html).contains("href=\"/insects/clades/lepidoptera\"");
        assertThat(html).doesNotContain("href=\"/clades/");
    }
}
