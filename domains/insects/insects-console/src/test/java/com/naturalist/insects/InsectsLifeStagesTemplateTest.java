package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.data.NaturalistTestExtension;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code life-stages.jte} heads a species with no common name the way {@code detail.jte} does. */
class InsectsLifeStagesTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void lifeStages_speciesWithoutCommonName_headsWithTheBinomialNotTheSlug() {
        InsectSpecies species = nte.getNamed(InsectSpeciesTestEntitySource.class)
                .getByName(TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name).orElseThrow();
        InsectGenus genus = nte.getNamed(InsectGenusTestEntitySource.class).getByName(species.genusName()).orElseThrow();
        InsectFamily family = nte.getNamed(InsectFamilyTestEntitySource.class).getByName(genus.familyName()).orElseThrow();
        InsectOrder order = nte.getNamed(InsectOrderTestEntitySource.class).getByName(family.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/life-stages.jte",
                Map.of("species", species, "genus", genus, "family", family, "order", order,
                        "stages", List.of()),
                output);

        assertThat(species.commonNames()).isEmpty();
        assertThat(output.toString()).contains("<h1>" + species.binomial(genus) + "</h1>")
                .doesNotContain("<h1>" + species.name().value() + "</h1>");
    }
}
