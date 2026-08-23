package com.naturalist.chemistry;

import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import com.naturalist.data.NaturalistDatabase;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code chemistry/detail.jte}. Renders every catalogued
 * compound through the template — every nullable branch and every nested
 * tree node executes against real data, catching api drift in any profile.
 */
class ChemistryDetailTemplateTest {

    @Test
    void detail_rendersEveryCompoundWithoutError() {
        var template = TestTemplateEngine.create();
        for (Compound compound : new CompoundTestEntitySource(NaturalistDatabase.create()).entityStream().toList()) {
            StringOutput output = new StringOutput();
            template.render("chemistry/detail.jte", Map.of("compound", compound), output);
            assertThat(output.toString())
                    .as("rendered output for %s", compound.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void detail_linksConstituentElementsThatAreCatalogued() {
        var db = NaturalistDatabase.create();
        Compound gypsum = new CompoundTestEntitySource(db).entityStream()
                .filter(c -> c.name().value().equals("calcium-sulfate-dihydrate"))
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/detail.jte",
                Map.of("compound", gypsum,
                        "linkableElements", Set.of("calcium", "sulfur", "oxygen", "hydrogen")),
                output);

        String html = output.toString();
        assertThat(html).contains("/chemistry/elements/calcium");
        assertThat(html).contains("/chemistry/elements/sulfur");
        assertThat(html).contains("Calcium");
    }

    @Test
    void detail_leavesUncataloguedElementsAsPlainText() {
        var db = NaturalistDatabase.create();
        Compound gypsum = new CompoundTestEntitySource(db).entityStream()
                .filter(c -> c.name().value().equals("calcium-sulfate-dihydrate"))
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        // Only calcium is catalogued: sulfur, oxygen and hydrogen must not become links.
        TestTemplateEngine.create().render("chemistry/detail.jte",
                Map.of("compound", gypsum, "linkableElements", Set.of("calcium")),
                output);

        String html = output.toString();
        assertThat(html).contains("/chemistry/elements/calcium");
        assertThat(html).doesNotContain("/chemistry/elements/sulfur");
        assertThat(html).contains("Sulfur");
    }
}
