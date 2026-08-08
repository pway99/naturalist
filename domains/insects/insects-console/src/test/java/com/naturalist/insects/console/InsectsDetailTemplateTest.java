package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectFamilyTestEntitySource;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusTestEntitySource;
import com.naturalist.insects.InsectOrder;
import com.naturalist.insects.InsectOrderTestEntitySource;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code detail.jte} carries the species page's own {@code CsrfToken}-typed
 * forms (observe / add-photo / field-notes), historically the one rank
 * template not covered by a render test because {@code CsrfToken} was not on
 * {@code insects-console}'s classpath. Now that it reads the CSRF param/token
 * as plain request attributes (matching {@code family.jte}, {@code genus.jte},
 * {@code order.jte}), it can be rendered here like the other rank pages.
 */
class InsectsDetailTemplateTest {

    @Test
    void detail_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        InsectFamilyTestEntitySource familySource = new InsectFamilyTestEntitySource(database);
        InsectGenusTestEntitySource genusSource = new InsectGenusTestEntitySource(database);
        InsectSpecies anySpecies = new InsectSpeciesTestEntitySource(database)
                .entityStream().findFirst().orElseThrow();
        InsectGenus genus = genusSource.getByName(anySpecies.genusName()).orElseThrow();
        InsectFamily family = familySource.getByName(genus.familyName()).orElseThrow();
        InsectOrder order = orderSource.getByName(family.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/detail.jte",
                Map.of(
                        "species", anySpecies,
                        "genus", genus,
                        "family", family,
                        "order", order,
                        "images", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
        assertThat(output.toString()).contains("Add Photo");
        // No request bound to the current thread -- _csrfParam/_csrfToken resolve to
        // null, so neither the observe-form nor the CSRF hidden input renders. This
        // is the behaviour the fix is verifying doesn't crash the template.
        assertThat(output.toString()).doesNotContain("observe-form");
    }
}
