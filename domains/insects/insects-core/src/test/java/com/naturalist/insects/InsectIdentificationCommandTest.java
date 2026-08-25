package com.naturalist.insects;

import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.authority.OnlineSource;
import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.PageRequest;
import com.naturalist.ddd.EntityName;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import com.naturalist.textgeneration.NoOpTextGenerationService;
import com.naturalist.vision.Image;
import com.naturalist.vision.ImageMetadata;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionExchange;
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
              "alternatives": null,
              "referenceUrl": "https://eol.org/pages/test-fabricatus"
            }
            """;

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(nte);
    InsectQuery query = context.insectQuery();

    private final VisionService stubService = (image, tool, prompt) ->
            fixedExchange("propose_insect_species", SAMPLE_RESULT_JSON);

    /**
     * A single-turn {@link VisionExchange} whose {@link VisionExchange#result()} is fixed.
     * These tests never run a second turn, so {@link VisionExchange#respond} fails loudly.
     */
    private static VisionExchange fixedExchange(String toolName, String argumentsJson) {
        return new VisionExchange() {
            @Override public ToolResult result() {
                return new ToolResult(toolName, argumentsJson);
            }
            @Override public VisionExchange respond(String toolResultJson, ToolSchema nextTool) {
                throw new AssertionError("no second turn expected in this test");
            }
        };
    }

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
        var libraryContext = LibraryTestContext.create(nte);
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

        assertThat(rankName.value()).isEqualTo("testus-fabricatus");

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

    /**
     * Before {@code InsectCatalogIdentificationTransaction} switched feature
     * persistence to {@code save()}, a re-identification of the same rank threw
     * {@code UniqueConstraintException} the instant vision reported a feature
     * string already in the catalog -- {@code InsectFeature.value} is
     * uniquely constrained and the command minted a fresh id and called
     * {@code insert()} unconditionally, every time, for every value. That threw
     * inside the (single) insect transaction, failing the whole identification,
     * not just the repeated feature. This test re-identifies the same species
     * from the same stub -- so vision reports the exact same three feature
     * strings both times -- and asserts: it does not throw, the feature catalog
     * does not grow (exact-string match reuses the existing rows; this is not
     * semantic dedup -- different phrasings of "the same" feature remain
     * separate rows), the assignment catalog does not grow either (the
     * (featureId, rankName) pair from the second call collides with the
     * first), and -- the sharp edge -- every persisted assignment's
     * {@code featureId} resolves to a real, persisted {@code InsectFeature}
     * row rather than the fresh id {@code resolveFeatures()} originally minted
     * and {@code save()} then discarded in favour of the existing one.
     */
    @Test
    void identify_reIdentifyingSameRankWithSameFeatures_doesNotGrowCatalogOrDangleFeatureIds() {
        var image = new Image(
                new byte[]{1, 2, 3}, "image/jpeg",
                new ImageMetadata("Chico, CA", null));

        command.identify(image, FileName.of("IMG_0006.jpg"), NaturalistName.of("pat"), null);

        var featureSource = nte.getNamed(InsectFeatureTestEntitySource.class);
        var assignmentSource = nte.getNamed(InsectFeatureAssignmentTestEntitySource.class);
        long featureCountAfterFirst = featureSource.entityStream().count();
        long assignmentCountAfterFirst = assignmentSource.entityStream().count();
        assertThat(featureCountAfterFirst).isGreaterThanOrEqualTo(3);

        // Same stub -> vision reports the exact same three feature strings again.
        command.identify(image, FileName.of("IMG_0007.jpg"), NaturalistName.of("pat"), null);

        assertThat(featureSource.entityStream().count()).isEqualTo(featureCountAfterFirst);
        assertThat(assignmentSource.entityStream().count()).isEqualTo(assignmentCountAfterFirst);

        var persistedFeatureIds = featureSource.entityStream()
                .map(InsectFeature::id)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(assignmentSource.entityStream().map(OrganismFeatureAssignment::featureId))
                .as("every assignment's featureId must resolve to a persisted InsectFeature -- "
                        + "no dangling FK left over from save()'s id reconciliation")
                .allMatch(persistedFeatureIds::contains);
    }

    @Test
    void identify_persistsCitationAssociationWhenCitationAlreadyExists() {
        var libraryContext = LibraryTestContext.create(nte);
        var cmd = new InsectIdentificationCommand(
                stubService,
                new NoOpTextGenerationService(),
                STUB_AUTHORITY,
                libraryContext.libraryCommand(),
                query,
                context.catalogIdentificationTransaction());

        // Pre-seed the deterministic citation slug the command derives for the
        // identified species rank ("test-testus-fabricatus" = source id "test" +
        // rank slug) with no matching association yet -- this is the state a
        // second identification of an already-cited rank produces: the citation
        // already exists, but this call must still attribute (create the
        // association for) the rank it identifies.
        var citationName = CitationName.of("test-testus-fabricatus");
        var preExisting = new OnlineSource(
                citationName,
                new AuthorityReference(new AuthoritySource("test", "Test Authority"),
                        URI.create("https://test.example.com/testus-fabricatus")),
                "Pre-existing citation", null, null, null);
        libraryContext.libraryCommand().citations().insert(preExisting);

        var image = new Image(
                new byte[]{1, 2, 3}, "image/jpeg",
                new ImageMetadata("Chico, CA", null));

        cmd.identify(image, FileName.of("IMG_0005.jpg"), NaturalistName.of("pat"), null);

        var associations = libraryContext.citationAssociationQuery().findByCitationName(citationName);
        assertThat(associations.size()).isEqualTo(1);
    }

    @Test
    void identify_parsesAlternativesJsonArrayIntoTypedCandidates() {
        var jsonWithAlternatives = SAMPLE_RESULT_JSON.replace(
                "\"alternatives\": null",
                "\"alternatives\": \"[{\\\"name\\\": \\\"Oncopeltus fasciatus\\\", "
                        + "\\\"commonName\\\": \\\"Large Milkweed Bug\\\", \\\"confidence\\\": 0.12}]\"");
        VisionService withAlternatives = (image, tool, prompt) ->
                fixedExchange("propose_insect_species", jsonWithAlternatives);
        var cmd = buildCommand(withAlternatives);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        var rankName = cmd.identify(
                image, FileName.of("IMG_0002.jpg"),
                NaturalistName.of("pat"), null);

        var obs = query.observations().findPage(PageRequest.console(0));
        var withId = obs.content().stream()
                .filter(o -> o.subject().equals(rankName)
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
            return fixedExchange("propose_insect_species", SAMPLE_RESULT_JSON);
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
            return fixedExchange("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var cmd = buildCommand(capturing);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        cmd.identify(image, FileName.of("IMG_0004.jpg"),
                NaturalistName.of("pat"), null);

        assertThat(promptCapture[0]).doesNotContain("Location context:");
    }
}
