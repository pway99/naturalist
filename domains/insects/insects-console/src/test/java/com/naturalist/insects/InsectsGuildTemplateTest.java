package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.insects.FunctionalGuild;
import com.naturalist.insects.InsectFunctionalRole;
import com.naturalist.insects.InsectFunctionalRoleTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    @Test
    void guild_rendersWithoutError() {
        FunctionalGuild selected = FunctionalGuild.POLLINATOR;
        List<InsectFunctionalRole> roles =
                db.getNamed(InsectFunctionalRoleTestEntitySource.class).entityStream()
                        .filter(r -> r.guilds().contains(selected))
                        .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/guild.jte",
                Map.of(
                        "selectedGuild", selected,
                        "guilds", FunctionalGuild.values(),
                        "roles", roles),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
