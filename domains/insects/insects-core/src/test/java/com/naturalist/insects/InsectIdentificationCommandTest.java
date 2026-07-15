package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.vision.Image;
import com.naturalist.vision.ImageMetadata;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.VisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class InsectIdentificationCommandTest {

    static final String SAMPLE_RESULT_JSON = """
            {
              "name": "vanessa-cardui",
              "order": "Lepidoptera",
              "family": "Nymphalidae",
              "genus": "Vanessa",
              "species": "cardui",
              "commonName": "Painted Lady",
              "descriptionPreschool": "A pretty orange and black butterfly with spots on its wings.",
              "descriptionElementary": "The Painted Lady is one of the most widespread butterflies in the world, found on every continent except Antarctica.",
              "descriptionSecondary": "Vanessa cardui is a highly migratory species known for its remarkable long-distance movements across continents.",
              "descriptionUniversity": "V. cardui exhibits one of the longest insect migration patterns known, with multi-generational movements spanning thousands of kilometers.",
              "guilds": ["POLLINATOR", "MIGRATORY"],
              "beneficial": true,
              "sightingNotes": "Nectaring on lantana in afternoon sun",
              "confidence": 0.85,
              "evidence": "Orange and black wing pattern with distinctive white spots on dark wing tips, four small eyespots on hindwing underside",
              "alternatives": null
            }
            """;

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContext context = InsectsTestContext.create(db);

    private final VisionService stubService = (image, tool, prompt) ->
            new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);

    private final InsectIdentificationCommand command = new InsectIdentificationCommand(
            stubService, context.catalogIdentificationTransaction());

    @Test
    void identify_returnsSpeciesNameAndPersistsEntities() {
        var image = new Image(
                new byte[]{1, 2, 3}, "image/jpeg",
                new ImageMetadata("Chico, CA", null));

        var speciesName = command.identify(
                image, FileName.of("IMG_0001.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(speciesName.value()).isEqualTo("vanessa-cardui");

        // Species persisted with parsed description
        var species = context.insectQuery().species().getByName(speciesName);
        assertThat(species).isPresent();
        assertThat(species.get().description().preschool()).contains("orange and black");

        // Parent ranks created
        assertThat(context.insectQuery().orders().getByName(InsectOrderName.of("lepidoptera")))
                .isPresent();
        assertThat(context.insectQuery().families().getByName(InsectFamilyName.of("nymphalidae")))
                .isPresent();
        assertThat(context.insectQuery().genera().getByName(InsectGenusName.of("vanessa")))
                .isPresent();

        // Image persisted
        var images = context.insectQuery().images().forParentName(speciesName);
        assertThat(images.stream().toList()).hasSize(1);
        assertThat(images.stream().toList().getFirst().resourceName())
                .isEqualTo(FileName.of("IMG_0001.jpg"));
    }

    @Test
    void identify_parsesAlternativesJsonArrayIntoTypedCandidates() {
        var jsonWithAlternatives = SAMPLE_RESULT_JSON.replace(
                "\"alternatives\": null",
                "\"alternatives\": \"[{\\\"name\\\": \\\"Oncopeltus fasciatus\\\", "
                        + "\\\"commonName\\\": \\\"Large Milkweed Bug\\\", \\\"confidence\\\": 0.12}]\"");
        VisionService withAlternatives = (image, tool, prompt) ->
                new ToolResult("propose_insect_species", jsonWithAlternatives);
        var cmd = new InsectIdentificationCommand(
                withAlternatives, context.catalogIdentificationTransaction());
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        var speciesName = cmd.identify(
                image, FileName.of("IMG_0002.jpg"),
                NaturalistName.of("pat"), null);

        // Observation carries the identification with alternatives
        var obsPage = context.insectQuery().fieldObservations()
                .findPage(com.naturalist.data.PageRequest.console(0));
        var withId = obsPage.content().stream()
                .filter(o -> o.identification() != null)
                .findFirst();
        assertThat(withId).isPresent();
        assertThat(withId.get().identification().alternatives()).hasSize(1);
        assertThat(withId.get().identification().alternatives().getFirst().scientificName())
                .isEqualTo("Oncopeltus fasciatus");
    }

    @Test
    void identify_includesLocationInPrompt() {
        var promptCapture = new String[1];
        VisionService capturing = (image, tool, prompt) -> {
            promptCapture[0] = prompt;
            return new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var cmd = new InsectIdentificationCommand(
                capturing, context.catalogIdentificationTransaction());
        var image = new Image(
                new byte[]{1}, "image/jpeg",
                new ImageMetadata("Deer Creek, Butte County, CA", null));

        cmd.identify(image, FileName.of("IMG_0003.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(promptCapture[0]).contains("Deer Creek, Butte County, CA");
    }

    @Test
    void identify_omitsLocationWhenNull() {
        var promptCapture = new String[1];
        VisionService capturing = (image, tool, prompt) -> {
            promptCapture[0] = prompt;
            return new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var cmd = new InsectIdentificationCommand(
                capturing, context.catalogIdentificationTransaction());
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        cmd.identify(image, FileName.of("IMG_0004.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(promptCapture[0]).doesNotContain("Location context:");
    }
}
