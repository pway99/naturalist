package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PlantPhytochemicalConstituentTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/phytochemistry/detail.jte}. Renders every
 * catalogued constituent — exercises every role-permit instanceof branch,
 * every nullable component, every tissue value, and the full description
 * tree per fixture.
 */
class PhytochemistryDetailTemplateTest {

    @Test
    void phytochemistryDetail_rendersEveryConstituentWithoutError() {
        var template = TestTemplateEngine.create();
        for (PhytochemicalConstituent constituent : new PlantPhytochemicalConstituentTestEntitySource(NaturalistDatabase.create()).entityStream().toList()) {
            StringOutput output = new StringOutput();
            template.render("plants/phytochemistry/detail.jte",
                    Map.of("constituent", constituent), output);
            assertThat(output.toString())
                    .as("rendered output for %s", constituent.name().value())
                    .isNotBlank();
        }
    }
}
