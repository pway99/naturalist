package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsGeneraTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void genera_rendersWithoutError() {
        Page<InsectGenus> genusPage = nte.getNamed(InsectGenusTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        Map<InsectFamilyName, InsectFamily> familyByName = nte.getNamed(InsectFamilyTestEntitySource.class)
                .entityStream().collect(Collectors.toMap(InsectFamily::name, f -> f));
        List<InsectViews.RankCard> cards = genusPage.content().stream()
                .map(g -> {
                    InsectFamily family = familyByName.get(g.familyName());
                    return new InsectViews.RankCard(g.name(), g.displayName(), g.genus().value(), g.familyName(),
                            family != null ? family.family().value() : g.familyName().value(),
                            InsectEntityCollections.ImageCollection.empty());
                })
                .toList();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/genera.jte",
                Map.of("cards", new Page<>(cards, genusPage.pageNumber(), genusPage.pageSize(),
                        genusPage.pagesAheadKnown(), genusPage.moreBeyondLookahead())),
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

        ConsoleSliceTemplates.create().render(
                "insects/genus.jte",
                Map.of(
                        "page", genusPage(anyGenus, family, order, List.of()),
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
        InsectTaxonView speciesChild = InsectTaxonView.SpeciesView.of(
                anySpecies,
                InsectEntityCollections.ImageCollection.empty());
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/genus.jte",
                Map.of(
                        "page", genusPage(genus, family, order, List.of(speciesChild)),
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

    private static InsectViews.RankPage genusPage(InsectGenus genus, InsectFamily family, InsectOrder order,
                                                  List<InsectTaxonView> children) {
        return InsectViews.RankPage.anonymous(Insect.empty()
                .withOrder(InsectTaxonView.OrderView.of(order))
                .withFamily(InsectTaxonView.FamilyView.of(family))
                .withGenus(InsectTaxonView.GenusView.of(genus))
                .withChildren(children));
    }
}
