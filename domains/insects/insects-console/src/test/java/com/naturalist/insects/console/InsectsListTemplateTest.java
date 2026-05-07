package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code insects/list.jte}. Asserts the template renders against
 * real fixtures — catches {@code @param} type drift, missing accessors, and
 * template compile errors caused by api changes. DOM/markup assertions are
 * intentionally omitted; presentation is QA'd manually.
 */
class InsectsListTemplateTest {

    @Test
    void list_rendersWithoutError() {
        Page<InsectSpecies> speciesPage = new InsectSpeciesTestEntitySource(NaturalistDatabase.create())
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/list.jte",
                Map.of("speciesPage", speciesPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
