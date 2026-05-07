package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.FunctionalGuild;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code insects/guild.jte}. Asserts the template renders against
 * real fixtures — catches {@code @param} type drift, missing accessors, and
 * template compile errors caused by api changes. DOM/markup assertions are
 * intentionally omitted; presentation is QA'd manually.
 */
class InsectsGuildTemplateTest {

    @Test
    void guild_rendersWithoutError() {
        FunctionalGuild selected = FunctionalGuild.POLLINATOR;
        List<InsectSpecies> species = new InsectSpeciesTestEntitySource(NaturalistDatabase.create()).entityStream()
                .filter(s -> s.guilds().contains(selected))
                .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/guild.jte",
                Map.of(
                        "selectedGuild", selected,
                        "guilds", FunctionalGuild.values(),
                        "species", species),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
