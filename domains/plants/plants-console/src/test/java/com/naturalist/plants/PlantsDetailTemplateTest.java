package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/detail.jte}. Renders every catalogued
 * plant through the template — every nullable branch and every nested
 * tree node executes against real data, catching api drift.
 */
class PlantsDetailTemplateTest {

    @Test
    void detail_rendersEveryPlantWithoutError() {
        var template = TestTemplateEngine.create();
        for (PlantSpecies plant : new PlantSpeciesTestEntitySource(NaturalistDatabase.create()).entityStream().toList()) {
            StringOutput output = new StringOutput();
            template.render("plants/detail.jte", Map.of("plant", plant), output);
            assertThat(output.toString())
                    .as("rendered output for %s", plant.name().value())
                    .isNotBlank();
        }
    }
}
