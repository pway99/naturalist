package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PlantPhytochemicalConstituentTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

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
        Page<PhytochemicalConstituent> constituentsPage = new PlantPhytochemicalConstituentTestEntitySource(
                NaturalistDatabase.create()).pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("plants/phytochemistry/list.jte",
                Map.of("constituentsPage", constituentsPage), output);

        assertThat(output.toString()).isNotBlank();
    }
}
