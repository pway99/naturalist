package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PlantPhytochemicalConstituentTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/phytochemistry/detail.jte}. Renders every
 * catalogued constituent — exercises every role-permit instanceof branch,
 * every nullable component, every tissue value, and the full description
 * tree per fixture.
 */
class PhytochemistryDetailTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void phytochemistryDetail_rendersEveryConstituentWithoutError() {
        var template = TestTemplateEngine.create();
        for (PhytochemicalConstituent constituent : nte.getNamed(PlantPhytochemicalConstituentTestEntitySource.class).entityStream().toList()) {
            StringOutput output = new StringOutput();
            template.render("plants/phytochemistry/detail.jte",
                    Map.of("constituent", constituent), output);
            assertThat(output.toString())
                    .as("rendered output for %s", constituent.name().value())
                    .isNotBlank();
        }
    }
}
