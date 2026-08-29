package com.naturalist.chemistry;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code chemistry/list.jte}. Asserts the template renders
 * against real fixtures — catches {@code @param} type drift, missing
 * accessors, and template compile errors caused by api changes.
 */
class ChemistryListTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void list_rendersWithoutError() {
        Page<Compound> compoundsPage = nte.getNamed(CompoundTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "chemistry/list.jte",
                Map.of("compoundsPage", compoundsPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
