package com.naturalist.insects;

import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.PageRequest;
import com.naturalist.ddd.EntityName;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.textgeneration.NoOpTextGenerationService;
import com.naturalist.vision.Image;
import com.naturalist.vision.ImageMetadata;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.VisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectIdentificationCommandTest {

    static final String SAMPLE_RESULT_JSON = """
            {
              "name": "testus-fabricatus",
              "identifiedRank": "SPECIES",
              "order": "Diptera",
              "family": "Syrphidae",
              "genus": "Testus",
              "species": "fabricatus",
              "commonName": "Fabricated Hover Fly",
              "descriptionPreschool": "A tiny fly that hovers in the air like a helicopter.",
              "descriptionElementary": "The Fabricated Hover Fly is an imaginary species used for testing.",
              "descriptionSecondary": "Testus fabricatus is a fictitious Syrphid fly created for test purposes.",
              "descriptionUniversity": "T. fabricatus does not exist outside of unit tests.",
              "guilds": ["POLLINATOR"],
              "beneficial": true,
              "features": ["hovering flight", "yellow-black banding", "large compound eyes"],
              "sightingNotes": "Hovering near test fixture",
              "confidence": 0.85,
              "evidence": "Distinctive test coloration with unmistakable fabricated wing venation",
              "alternatives": null
            }
            """;

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(db);
    InsectQuery query = context.insectQuery();

    private final VisionService stubService = (image, tool, prompt) ->
            new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);

    /**
     * Stub authority that confirms any entity name it is asked about.
     */
    private static final ExternalAuthority STUB_AUTHORITY = new ExternalAuthority() {
        private static final AuthoritySource SOURCE =
                new AuthoritySource("test", "Test Authority");

        @Override
        public AuthoritySource source() {
            return SOURCE;
        }

        @Override
        public Set<AuthorityReference> lookup(EntityName subject) {
            return Set.of(new AuthorityReference(SOURCE,
                    URI.create("https://test.example.com/" + subject.value())));
        }

        @Override
        public AuthorityContent fetchContent(AuthorityReference ref) {
            return new AuthorityContent(ref, "Stub content for " + ref.url());
        }
    };

    private InsectIdentificationCommand buildCommand(VisionService vision) {
        var libraryContext = LibraryTestContext.create(db);
        return new InsectIdentificationCommand(
                vision,
                new NoOpTextGenerationService(),
                STUB_AUTHORITY,
                libraryContext.libraryCommand(),
                query,
                context.catalogIdentificationTransaction());
    }

    private final InsectIdentificationCommand command = buildCommand(stubService);

    @Test
    void identify_returnsRankNameAndPersistsEntities() {
        var image = new Image(
                new byte[]{1, 2, 3}, "image/jpeg",
                new ImageMetadata("Chico, CA", null));

        var rankName = command.identify(
                image, FileName.of("IMG_0001.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(rankName).isPresent();
        assertThat(rankName.get().value()).isEqualTo("testus-fabricatus");

        // Species persisted with parsed description
        var speciesName = InsectSpeciesName.of("testus-fabricatus");
        var species = query.species().getByName(speciesName);
        assertThat(species).isPresent();
        assertThat(species.get().description().preschool()).contains("hovers in the air");

        // Parent ranks created
        assertThat(query.orders().getByName(InsectOrderName.of("diptera"))).isPresent();
        assertThat(query.families().getByName(InsectFamilyName.of("syrphidae"))).isPresent();
        assertThat(query.genera().getByName(InsectGenusName.of("testus"))).isPresent();

        // Image persisted
        var images = query.images().forParentName(speciesName);
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
        var cmd = buildCommand(withAlternatives);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        var rankName = cmd.identify(
                image, FileName.of("IMG_0002.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(rankName).isPresent();

        var obs = query.fieldObservations().findPage(PageRequest.console(0));
        var withId = obs.content().stream()
                .filter(o -> o.subject().equals(rankName.get())
                        && o.identification() != null)
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
        var cmd = buildCommand(capturing);
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
        var cmd = buildCommand(capturing);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        cmd.identify(image, FileName.of("IMG_0004.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(promptCapture[0]).doesNotContain("Location context:");
    }
}
