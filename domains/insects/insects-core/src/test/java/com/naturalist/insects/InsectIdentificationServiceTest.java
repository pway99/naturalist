package com.naturalist.insects;

import com.naturalist.vision.Image;
import com.naturalist.vision.ImageMetadata;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsectIdentificationServiceTest {

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

    private final VisionService stubService = (image, tool, prompt) ->
            new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);

    private final InsectIdentificationService service = new InsectIdentificationService(stubService);

    @Test
    void identify_parsesSpeciesFromToolResult() {
        var image = new Image(
                new byte[]{1, 2, 3}, "image/jpeg",
                new ImageMetadata("Chico, CA", null));

        var result = service.identify(image);

        assertThat(result.species().name().value()).isEqualTo("vanessa-cardui");
        assertThat(result.species().description().preschool()).contains("orange and black");
        assertThat(result.identification().confidence()).isEqualTo(0.85);
        assertThat(result.identification().evidence()).contains("wing pattern");
        assertThat(result.identification().alternatives()).isEmpty();
    }

    @Test
    void identify_parsesAlternativesJsonArrayIntoTypedCandidates() {
        var jsonWithAlternatives = SAMPLE_RESULT_JSON.replace(
                "\"alternatives\": null",
                "\"alternatives\": \"[{\\\"name\\\": \\\"Oncopeltus fasciatus\\\", "
                        + "\\\"commonName\\\": \\\"Large Milkweed Bug\\\", \\\"confidence\\\": 0.12}]\"");
        VisionService withAlternatives = (image, tool, prompt) ->
                new ToolResult("propose_insect_species", jsonWithAlternatives);
        var svc = new InsectIdentificationService(withAlternatives);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        var alternatives = svc.identify(image).identification().alternatives();

        assertThat(alternatives).hasSize(1);
        assertThat(alternatives.getFirst().scientificName()).isEqualTo("Oncopeltus fasciatus");
        assertThat(alternatives.getFirst().commonName()).isEqualTo("Large Milkweed Bug");
        assertThat(alternatives.getFirst().confidence()).isEqualTo(0.12);
    }

    @Test
    void identify_includesLocationInPrompt() {
        var promptCapture = new String[1];
        VisionService capturing = (image, tool, prompt) -> {
            promptCapture[0] = prompt;
            return new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var svc = new InsectIdentificationService(capturing);
        var image = new Image(
                new byte[]{1}, "image/jpeg",
                new ImageMetadata("Deer Creek, Butte County, CA", null));

        svc.identify(image);

        assertThat(promptCapture[0]).contains("Deer Creek, Butte County, CA");
    }

    @Test
    void identify_omitsLocationWhenNull() {
        var promptCapture = new String[1];
        VisionService capturing = (image, tool, prompt) -> {
            promptCapture[0] = prompt;
            return new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var svc = new InsectIdentificationService(capturing);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        svc.identify(image);

        assertThat(promptCapture[0]).doesNotContain("Location context:");
    }
}
