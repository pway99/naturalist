package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code chemistry/list.jte}. Asserts the template renders
 * against real fixtures — catches {@code @param} type drift, missing
 * accessors, and template compile errors caused by api changes.
 */
class ChemistryListTemplateTest {

    @Test
    void list_rendersWithoutError() {
        Page<Compound> compoundsPage = new CompoundTestEntitySource(NaturalistDatabase.create())
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "chemistry/list.jte",
                Map.of("compoundsPage", compoundsPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
