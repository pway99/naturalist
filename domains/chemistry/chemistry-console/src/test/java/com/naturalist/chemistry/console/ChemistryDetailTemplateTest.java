package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code chemistry/detail.jte}. Renders every catalogued
 * compound through the template — every nullable branch and every nested
 * tree node executes against real data, catching api drift in any profile.
 */
class ChemistryDetailTemplateTest {

    @Test
    void detail_rendersEveryCompoundWithoutError() {
        var template = TestTemplateEngine.create();
        for (Compound compound : new CompoundTestEntitySource().entityStream().toList()) {
            StringOutput output = new StringOutput();
            template.render("chemistry/detail.jte", Map.of("compound", compound), output);
            assertThat(output.toString())
                    .as("rendered output for %s", compound.name().value())
                    .isNotBlank();
        }
    }
}
