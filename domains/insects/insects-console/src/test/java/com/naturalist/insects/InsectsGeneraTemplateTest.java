package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsGeneraTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void genera_rendersWithoutError() {
        Page<InsectGenus> genusPage = nte.getNamed(InsectGenusTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        InsectFamilyTestEntitySource familySource = nte.getNamed(InsectFamilyTestEntitySource.class);
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
        InsectOrderTestEntitySource orderSource = nte.getNamed(InsectOrderTestEntitySource.class);
        InsectGenusTestEntitySource genusSource = nte.getNamed(InsectGenusTestEntitySource.class);
        InsectGenus anyGenus = genusSource.entityStream().findFirst().orElseThrow();
        InsectFamily family = nte.getNamed(InsectFamilyTestEntitySource.class)
                .getByName(anyGenus.familyName()).orElseThrow();
        InsectOrder order = orderSource.getByName(family.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", anyGenus,
                        "family", family,
                        "order", order,
                        "children", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void genus_rendersChildSpeciesCardFromPermit() {
        InsectOrderTestEntitySource orderSource = nte.getNamed(InsectOrderTestEntitySource.class);
        InsectSpecies anySpecies = nte.getNamed(InsectSpeciesTestEntitySource.class).entityStream()
                .findFirst().orElseThrow();
        InsectGenus genus = nte.getNamed(InsectGenusTestEntitySource.class)
                .getByName(anySpecies.genusName()).orElseThrow();
        InsectFamily family = nte.getNamed(InsectFamilyTestEntitySource.class)
                .getByName(genus.familyName()).orElseThrow();
        InsectOrder order = orderSource.getByName(family.orderName()).orElseThrow();
        InsectTaxonView speciesChild = InsectSpeciesView.of(
                anySpecies,
                InsectEntityCollections.ImageCollection.empty());
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", genus,
                        "family", family,
                        "order", order,
                        "children", List.of(speciesChild),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        String speciesDisplayName = anySpecies.commonNames().stream().findFirst()
                .map(cn -> cn.label()).orElse(anySpecies.name().value());
        assertThat(output.toString()).contains(speciesDisplayName);
        assertThat(output.toString()).contains("/insects/" + anySpecies.name().value());
    }
}
