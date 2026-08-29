package com.naturalist.plants;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/list.jte}. Asserts the template renders
 * against real fixtures — catches {@code @param} type drift, missing
 * accessors, and template compile errors caused by api changes.
 */
class PlantsListTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void list_rendersWithoutError() {
        Page<PlantSpecies> plantsPage = nte.getNamed(PlantSpeciesTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "plants/list.jte",
                Map.of("plantsPage", plantsPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
