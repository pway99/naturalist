package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.observation.OrganismImage;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code insects/list.jte}. Asserts the template renders against
 * real fixtures — catches {@code @param} type drift, missing accessors, and
 * template compile errors caused by api changes. DOM/markup assertions are
 * intentionally omitted; presentation is QA'd manually.
 */
class InsectsListTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void list_rendersWithoutError() {
        Page<InsectSpecies> speciesPage = nte.getNamed(InsectSpeciesTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        Map<InsectSpeciesName, List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>>> imagesBySpecies =
                nte.getNamed(InsectImageTestEntitySource.class).entityStream()
                        .filter(image -> image.parentName() instanceof InsectSpeciesName)
                        .collect(Collectors.groupingBy(image -> (InsectSpeciesName) image.parentName()));
        Map<InsectGenusName, InsectGenus> genusByName = nte.getNamed(InsectGenusTestEntitySource.class)
                .entityStream().collect(Collectors.toMap(InsectGenus::name, g -> g));
        Map<InsectFamilyName, InsectFamily> familyByName = nte.getNamed(InsectFamilyTestEntitySource.class)
                .entityStream().collect(Collectors.toMap(InsectFamily::name, f -> f));
        List<InsectViews.SpeciesCard> cards = speciesPage.content().stream()
                .map(s -> {
                    InsectGenus genus = genusByName.get(s.genusName());
                    InsectFamily family = genus == null ? null : familyByName.get(genus.familyName());
                    return new InsectViews.SpeciesCard(s, genus, family, null,
                            InsectEntityCollections.ImageCollection.of(
                                    imagesBySpecies.getOrDefault(s.name(), List.of())),
                            false);
                })
                .toList();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/list.jte",
                Map.of("cards", new Page<>(cards, speciesPage.pageNumber(), speciesPage.pageSize(),
                        speciesPage.pagesAheadKnown(), speciesPage.moreBeyondLookahead())),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
