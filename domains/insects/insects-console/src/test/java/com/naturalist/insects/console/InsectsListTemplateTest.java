package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectImage;
import com.naturalist.insects.InsectImageTestEntitySource;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectSpecies> speciesPage = new InsectSpeciesTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        Map<InsectSpeciesName, List<InsectImage>> imagesBySpecies =
                new InsectImageTestEntitySource(database).entityStream()
                        .collect(Collectors.groupingBy(InsectImage::insectSpeciesName));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/list.jte",
                Map.of(
                        "speciesPage", speciesPage,
                        "imagesBySpecies", imagesBySpecies),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
