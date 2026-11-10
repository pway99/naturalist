package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;


import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsFamiliesTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void families_rendersWithoutError() {
        Map<InsectOrderName, InsectOrderInfo> orderByName = nte.getNamed(InsectOrderTestEntitySource.class)
                .entityStream().collect(java.util.stream.Collectors.toMap(InsectOrderInfo::name, o -> o));
        Page<InsectFamilyInfo> familyPage = nte.getNamed(InsectFamilyTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        List<InsectViews.RankCard> cards = familyPage.content().stream()
                .map(f -> {
                    InsectOrderInfo order = orderByName.get(f.orderName());
                    return new InsectViews.RankCard(f.name(), f.displayName(), f.family().value(), f.orderName(),
                            order != null ? order.order().value() : f.orderName().value(),
                            InsectEntityCollections.ImageCollection.empty());
                })
                .toList();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/families.jte",
                Map.of("cards", new Page<>(cards, familyPage.pageNumber(), familyPage.pageSize(),
                        familyPage.pagesAheadKnown(), familyPage.moreBeyondLookahead())),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void family_rendersWithoutError() {
        InsectOrderTestEntitySource orderSource = nte.getNamed(InsectOrderTestEntitySource.class);
        InsectFamilyInfo anyFamily = nte.getNamed(InsectFamilyTestEntitySource.class).entityStream()
                .findFirst().orElseThrow();
        InsectOrderInfo order = orderSource.getByName(anyFamily.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/family.jte",
                Map.of(
                        "page", familyPage(anyFamily, order, List.of()),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void family_rendersRankImagesWithEvidence() {
        InsectOrderTestEntitySource orderSource = nte.getNamed(InsectOrderTestEntitySource.class);
        InsectFamilyInfo anyFamily = nte.getNamed(InsectFamilyTestEntitySource.class).entityStream()
                .findFirst().orElseThrow();
        InsectOrderInfo order = orderSource.getByName(anyFamily.orderName()).orElseThrow();
        var observationId = com.naturalist.insects.InsectObservationId.create();
        var image = new com.naturalist.observation.OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                com.naturalist.insects.InsectImageId.create(),
                anyFamily.name(),
                java.time.Instant.parse("2026-07-16T01:54:24Z"),
                com.naturalist.data.FileName.of("beetle.jpg"),
                observationId);
        var identification = new com.naturalist.observation.Identification(
                0.72,
                "Elytra pattern and antenna shape match this family's diagnostic features.",
                List.of(new com.naturalist.observation.Identification.Candidate(
                        "Coccinellidae", "Ladybird beetle", 0.20)));
        var observation = new com.naturalist.observation.OrganismObservation<InsectObservationId, InsectRankName>(
                observationId,
                com.naturalist.naturalist.NaturalistName.of("pat-way"),
                anyFamily.name(),
                java.time.Instant.parse("2026-07-16T01:54:24Z"),
                null,
                "Oak Vista, Chico, CA",
                null,
                identification);
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/family.jte",
                Map.of(
                        "page", new InsectViews.RankPage(
                                familyPage(anyFamily, order, List.of()).insect()
                                        .withObservations(InsectEntityCollections.ImageCollection.of(List.of(image))),
                                new InsectViews.Viewer(true,
                                        InsectEntityCollections.ImageCollection.of(List.of(image)),
                                        Map.of(image.id(), observation))),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).contains("Photo Gallery");
        assertThat(output.toString()).contains("beetle.jpg");
        assertThat(output.toString()).contains("obs-confidence-value\">72%");
        assertThat(output.toString()).contains("Why this ID?");
        assertThat(output.toString()).contains("name=\"returnPath\"");
    }

    @Test
    void family_rendersChildGenusCardFromPermit() {
        InsectOrderTestEntitySource orderSource = nte.getNamed(InsectOrderTestEntitySource.class);
        InsectGenusInfo anyGenus = nte.getNamed(InsectGenusTestEntitySource.class).entityStream()
                .findFirst().orElseThrow();
        InsectFamilyInfo family = nte.getNamed(InsectFamilyTestEntitySource.class)
                .getByName(anyGenus.familyName()).orElseThrow();
        InsectOrderInfo order = orderSource.getByName(family.orderName()).orElseThrow();
        InsectTaxon genusChild = InsectGenus.of(
                anyGenus,
                InsectEntityCollections.ImageCollection.empty());
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/family.jte",
                Map.of(
                        "page", familyPage(family, order, List.of(genusChild)),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        String genusDisplayName = anyGenus.displayName();
        assertThat(output.toString()).contains(genusDisplayName);
        assertThat(output.toString()).contains("/insects/genera/" + anyGenus.name().value());
    }

    private static InsectViews.RankPage familyPage(InsectFamilyInfo family, InsectOrderInfo order,
                                                   List<InsectTaxon> children) {
        return InsectViews.RankPage.anonymous(Insect.empty()
                .withOrder(InsectOrder.of(order))
                .withFamily(InsectFamily.of(family))
                .withChildren(children));
    }
}
