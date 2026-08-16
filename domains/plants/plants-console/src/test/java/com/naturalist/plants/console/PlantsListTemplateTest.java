package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.plants.PlantSpecies;
import com.naturalist.plants.PlantSpeciesTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/list.jte}. Asserts the template renders
 * against real fixtures — catches {@code @param} type drift, missing
 * accessors, and template compile errors caused by api changes.
 */
class PlantsListTemplateTest {

    @Test
    void list_rendersWithoutError() {
        Page<PlantSpecies> plantsPage = new PlantSpeciesTestEntitySource(NaturalistDatabase.create())
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "plants/list.jte",
                Map.of("plantsPage", plantsPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
