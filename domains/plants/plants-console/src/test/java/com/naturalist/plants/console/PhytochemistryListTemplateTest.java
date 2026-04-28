package com.naturalist.plants.console;

import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/phytochemistry/list.jte}. Catches drift
 * in {@code PhytochemicalConstituent}'s api or in the role-axis rollup
 * predicates that drive the badges.
 */
class PhytochemistryListTemplateTest {

    @Test
    void phytochemistryList_rendersWithoutError() {
        List<PhytochemicalConstituent> constituents = new PhytochemicalConstituentTestEntitySource().entityStream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value()))
                .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("plants/phytochemistry/list.jte",
                Map.of("constituents", constituents), output);

        assertThat(output.toString()).isNotBlank();
    }
}
