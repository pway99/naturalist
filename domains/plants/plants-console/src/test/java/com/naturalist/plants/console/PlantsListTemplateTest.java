package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.Plant;
import com.naturalist.plants.PlantTestEntitySource;

import gg.jte.output.StringOutput;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/list.jte}. Asserts the template renders
 * against real fixtures — catches {@code @param} type drift, missing
 * accessors, and template compile errors caused by api changes.
 */
class PlantsListTemplateTest {

    @Test
    void list_rendersWithoutError() {
        List<Plant> plants = new PlantTestEntitySource(NaturalistDatabase.create()).entityStream()
                .sorted(Comparator.comparing((Plant p) -> p.name().value()))
                .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("plants/list.jte", Map.of("plants", plants), output);

        assertThat(output.toString()).isNotBlank();
    }
}
