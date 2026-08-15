package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantFamily;
import com.naturalist.plants.PlantFamilyTestEntitySource;
import com.naturalist.plants.PlantGenus;
import com.naturalist.plants.PlantGenusTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/families/detail.jte}. Renders every catalogued
 * family — both the populated-genera branch and the empty-genera branch execute
 * against real data.
 */
class PlantsFamilyDetailTemplateTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();

    @Test
    void familyDetail_rendersEveryFamilyWithoutError() {
        var template = TestTemplateEngine.create();
        List<PlantGenus> allGenera =
                db.getNamed(PlantGenusTestEntitySource.class).entityStream().toList();

        for (PlantFamily family : db.getNamed(PlantFamilyTestEntitySource.class)
                .entityStream().toList()) {
            List<PlantGenus> genera = allGenera.stream()
                    .filter(g -> family.name().equals(g.familyName()))
                    .toList();
            StringOutput output = new StringOutput();
            template.render("plants/families/detail.jte",
                    Map.of("family", family, "genera", genera), output);
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
        List<PlantGenus> genera = db.getNamed(PlantGenusTestEntitySource.class).entityStream()
                .filter(g -> lamiaceae.name().equals(g.familyName()))
                .toList();

        StringOutput output = new StringOutput();
        template.render("plants/families/detail.jte",
                Map.of("family", lamiaceae, "genera", genera), output);

        assertThat(output.toString())
                .contains("/plants/genera/thymus")
                .contains("/plants/genera/salvia")
                .doesNotContain("No genera catalogued");
    }

    @Test
    void familyDetail_rendersEmptyStateWhenTheFamilyHasNoGenera() {
        var template = TestTemplateEngine.create();
        PlantFamily rosaceae = db.getNamed(PlantFamilyTestEntitySource.class).entityStream()
                .filter(f -> f.name().value().equals("rosaceae"))
                .findFirst()
                .orElseThrow();

        StringOutput output = new StringOutput();
        template.render("plants/families/detail.jte",
                Map.of("family", rosaceae, "genera", List.of()), output);

        assertThat(output.toString()).contains("No genera catalogued");
    }
}
