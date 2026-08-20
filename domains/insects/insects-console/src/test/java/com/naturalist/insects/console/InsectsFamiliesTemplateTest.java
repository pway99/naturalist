package com.naturalist.insects.console;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectEntityCollections;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusTestEntitySource;
import com.naturalist.insects.InsectGenusView;
import com.naturalist.insects.InsectImageId;
import com.naturalist.insects.InsectObservationId;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectFamilyTestEntitySource;
import com.naturalist.insects.InsectOrder;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectOrderTestEntitySource;
import com.naturalist.insects.InsectTaxonView;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsFamiliesTemplateTest {

    @Test
    void families_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        Page<InsectFamily> familyPage = new InsectFamilyTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        Map<InsectOrderName, InsectOrder> orderByName = new LinkedHashMap<>();
        for (var family : familyPage.content()) {
            orderByName.computeIfAbsent(family.orderName(),
                    n -> orderSource.getByName(n).orElseThrow());
        }
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/families.jte",
                Map.of("familyPage", familyPage,
                       "orderByName", orderByName),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void family_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectOrder order = orderSource.getByName(anyFamily.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", anyFamily,
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
    void family_rendersRankImagesWithEvidence() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectOrder order = orderSource.getByName(anyFamily.orderName()).orElseThrow();
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
                identification);
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", anyFamily,
                        "order", order,
                        "children", List.of(),
                        "images", List.of(image),
                        "observations", Map.of(image.id(), observation),
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
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        InsectGenus anyGenus = new InsectGenusTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectFamily family = new InsectFamilyTestEntitySource(database)
                .getByName(anyGenus.familyName()).orElseThrow();
        InsectOrder order = orderSource.getByName(family.orderName()).orElseThrow();
        InsectTaxonView genusChild = InsectGenusView.of(
                anyGenus,
                InsectEntityCollections.ImageCollection.empty());
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", family,
                        "order", order,
                        "children", List.of(genusChild),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        String genusDisplayName = anyGenus.commonNames().stream().findFirst()
                .map(cn -> cn.label()).orElse(anyGenus.name().value());
        assertThat(output.toString()).contains(genusDisplayName);
        assertThat(output.toString()).contains("/insects/genera/" + anyGenus.name().value());
    }
}
