package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsGeneraTemplateTest {

    @Test
    void genera_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectGenus> genusPage = new InsectGenusTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        InsectFamilyTestEntitySource familySource = new InsectFamilyTestEntitySource(database);
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        for (var genus : genusPage.content()) {
            familyByName.computeIfAbsent(genus.familyName(),
                    n -> familySource.getByName(n).orElseThrow());
        }
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genera.jte",
                Map.of("genusPage", genusPage,
                       "familyByName", familyByName),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void genus_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectGenusTestEntitySource genusSource = new InsectGenusTestEntitySource(database);
        InsectGenus anyGenus = genusSource.entityStream().findFirst().orElseThrow();
        InsectFamily family = new InsectFamilyTestEntitySource(database)
                .getByName(anyGenus.familyName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", anyGenus,
                        "family", family,
                        "species", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
