package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/families/list.jte} — the browse entry point
 * for family-rank records.
 */
class PlantsFamilyListTemplateTest {

    @Test
    void familyList_rendersWithoutError() {
        Page<PlantFamily> familiesPage = NaturalistDatabase.create()
                .getNamed(PlantFamilyTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "plants/families/list.jte",
                Map.of("familiesPage", familiesPage),
                output);

        assertThat(output.toString())
                .isNotBlank()
                .contains("/plants/families/lamiaceae");
    }
}
