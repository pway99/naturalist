package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsGeneraTemplateTest {

    @Test
    void genera_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectGenus> genusPage = new InsectGenusTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genera.jte",
                Map.of("genusPage", genusPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void genus_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectGenus anyGenus = new InsectGenusTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", anyGenus,
                        "species", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
