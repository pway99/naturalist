package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesTestEntitySource;

import gg.jte.output.StringOutput;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
        List<InsectSpecies> species = new InsectSpeciesTestEntitySource(NaturalistDatabase.create()).entityStream().toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("insects/list.jte", Map.of("species", species), output);

        assertThat(output.toString()).isNotBlank();
    }
}
