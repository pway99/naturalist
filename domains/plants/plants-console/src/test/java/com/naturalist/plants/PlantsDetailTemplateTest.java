package com.naturalist.plants;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.data.NaturalistTestExtension;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/detail.jte}. Renders every catalogued
 * plant through the template — every nullable branch and every nested
 * tree node executes against real data, catching api drift.
 */
class PlantsDetailTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void detail_rendersEveryPlantWithoutError() {
        var template = ConsoleSliceTemplates.create();
        for (PlantSpecies plant : nte.getNamed(PlantSpeciesTestEntitySource.class).entityStream().toList()) {
            StringOutput output = new StringOutput();
            template.render("plants/detail.jte", Map.of("plant", plant), output);
            assertThat(output.toString())
                    .as("rendered output for %s", plant.name().value())
                    .isNotBlank();
        }
    }
}
