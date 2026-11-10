package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.GridCell;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistTestExtension;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.List;
import java.time.Instant;

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

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void detail_rendersWithoutError() {
        InsectOrderTestEntitySource orderSource = nte.getNamed(InsectOrderTestEntitySource.class);
        InsectFamilyTestEntitySource familySource = nte.getNamed(InsectFamilyTestEntitySource.class);
        InsectGenusTestEntitySource genusSource = nte.getNamed(InsectGenusTestEntitySource.class);
        InsectSpeciesInfo anySpecies = nte.getNamed(InsectSpeciesTestEntitySource.class)
                .entityStream().findFirst().orElseThrow();
        InsectGenusInfo genus = genusSource.getByName(anySpecies.genusName()).orElseThrow();
        InsectFamilyInfo family = familySource.getByName(genus.familyName()).orElseThrow();
        InsectOrderInfo order = orderSource.getByName(family.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/detail.jte",
                Map.of(
                        "page", InsectViews.RankPage.anonymous(Insect.empty()
                                .withOrder(InsectOrder.of(order))
                                .withFamily(InsectFamily.of(family))
                                .withGenus(InsectGenus.of(genus))
                                .withSpecies(InsectSpecies.of(anySpecies))),
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

    @Test
    void detail_speciesWithoutCommonName_headsWithTheBinomialNotTheSlug() {
        InsectSpeciesInfo species = nte.getNamed(InsectSpeciesTestEntitySource.class)
                .getByName(TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name).orElseThrow();
        InsectGenusInfo genus = nte.getNamed(InsectGenusTestEntitySource.class).getByName(species.genusName()).orElseThrow();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/detail.jte",
                Map.of(
                        "page", InsectViews.RankPage.anonymous(Insect.empty()
                                .withGenus(InsectGenus.of(genus))
                                .withSpecies(InsectSpecies.of(species))),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(species.commonNames()).isEmpty();
        assertThat(output.toString()).contains("<h1>" + species.binomial(genus) + "</h1>")
                .doesNotContain("<h1>" + species.name().value() + "</h1>");
    }

    @Test
    void detail_addPhotoFormCarriesTheAreaFieldsAndAClearableAreaLine() {
        InsectSpeciesInfo species = nte.getNamed(InsectSpeciesTestEntitySource.class)
                .getByName(TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name).orElseThrow();
        InsectGenusInfo genus = nte.getNamed(InsectGenusTestEntitySource.class).getByName(species.genusName()).orElseThrow();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/detail.jte",
                Map.of(
                        "page", InsectViews.RankPage.anonymous(Insect.empty()
                                .withGenus(InsectGenus.of(genus))
                                .withSpecies(InsectSpecies.of(species))),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString())
                .contains("name=\"areaLat\"")
                .contains("name=\"areaLon\"")
                .contains("id=\"area-line\"")
                .contains("id=\"area-clear\"");
    }

    @Test
    void detail_observationWithAnArea_saysWhichAreaItWasObservedIn() {
        InsectSpeciesInfo species = nte.getNamed(InsectSpeciesTestEntitySource.class)
                .getByName(TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name).orElseThrow();
        InsectGenusInfo genus = nte.getNamed(InsectGenusTestEntitySource.class).getByName(species.genusName()).orElseThrow();
        var observationId = InsectObservationId.create();
        var image = new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                species.name(),
                Instant.parse("2026-07-16T01:54:24Z"),
                FileName.of("fly.jpg"),
                observationId);
        var observation = new OrganismObservation<InsectObservationId, InsectRankName>(
                observationId,
                NaturalistName.of("pat-way"),
                species.name(),
                Instant.parse("2026-07-16T01:54:24Z"),
                null,
                null,
                GridCell.of(1092, -2945),
                null);
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/detail.jte",
                Map.of(
                        "page", new InsectViews.RankPage(
                                Insect.empty()
                                        .withGenus(InsectGenus.of(genus))
                                        .withSpecies(InsectSpecies.of(species))
                                        .withObservations(InsectEntityCollections.ImageCollection.of(List.of(image))),
                                new InsectViews.Viewer(true,
                                        InsectEntityCollections.ImageCollection.of(List.of(image)),
                                        Map.of(image.id(), observation))),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).contains("Observed in the area 45.50–45.54°N, 122.71–122.67°W");
    }
}
