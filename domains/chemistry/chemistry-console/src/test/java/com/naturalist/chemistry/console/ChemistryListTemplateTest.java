package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import com.naturalist.data.NaturalistDatabase;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
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
        List<Compound> compounds = new CompoundTestEntitySource(NaturalistDatabase.create()).entityStream()
                .sorted(Comparator.comparing((Compound c) -> c.name().value()))
                .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/list.jte", Map.of("compounds", compounds), output);

        assertThat(output.toString()).isNotBlank();
    }
}
