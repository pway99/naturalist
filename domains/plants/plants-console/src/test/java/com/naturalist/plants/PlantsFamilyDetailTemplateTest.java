package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/families/detail.jte}. Renders every catalogued
 * family — both the populated-genera branch and the empty-genera branch execute
 * against real data.
 */
class PlantsFamilyDetailTemplateTest {

    @RegisterExtension
    private final NaturalistTestExtension db = NaturalistTestExtension.create();

    @Test
    void familyDetail_rendersEveryFamilyWithoutError() {
        var template = TestTemplateEngine.create();
        List<PlantGenus> allGenera =
                db.getNamed(PlantGenusTestEntitySource.class).entityStream().toList();

        for (PlantFamily family : db.getNamed(PlantFamilyTestEntitySource.class)
                .entityStream().toList()) {
            List<PlantTaxonView> children = allGenera.stream()
                    .filter(g -> family.name().equals(g.familyName()))
                    .map(g -> (PlantTaxonView) PlantGenusView.of(g))
                    .toList();
            StringOutput output = new StringOutput();
            template.render("plants/families/detail.jte",
                    Map.of("family", family, "children", children), output);
            assertThat(output.toString())
                    .as("rendered output for %s", family.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void familyDetail_rendersGeneraCardsWhenTheFamilyHasThem() {
        var template = TestTemplateEngine.create();
        PlantFamily lamiaceae = db.getNamed(PlantFamilyTestEntitySource.class).entityStream()
                .filter(f -> f.name().value().equals("lamiaceae"))
                .findFirst()
                .orElseThrow();
        List<PlantTaxonView> children = db.getNamed(PlantGenusTestEntitySource.class).entityStream()
                .filter(g -> lamiaceae.name().equals(g.familyName()))
                .map(g -> (PlantTaxonView) PlantGenusView.of(g))
                .toList();

        StringOutput output = new StringOutput();
        template.render("plants/families/detail.jte",
                Map.of("family", lamiaceae, "children", children), output);

        assertThat(output.toString())
                .contains("/plants/genera/thymus")
                .contains("/plants/genera/salvia")
                .doesNotContain("No genera catalogued");
    }

    @Test
    void familyDetail_rendersEmptyStateWhenNoGeneraAreSupplied() {
        // Exercises the template's empty branch, not a fact about the catalog:
        // every family currently has at least one genus, so the empty list is
        // supplied deliberately rather than found. Keeps the branch covered as
        // the genus catalog grows.
        var template = TestTemplateEngine.create();
        PlantFamily anyFamily = db.getNamed(PlantFamilyTestEntitySource.class).entityStream()
                .findFirst()
                .orElseThrow();

        StringOutput output = new StringOutput();
        template.render("plants/families/detail.jte",
                Map.of("family", anyFamily, "children", List.of()), output);

        assertThat(output.toString()).contains("No genera catalogued");
    }
}
