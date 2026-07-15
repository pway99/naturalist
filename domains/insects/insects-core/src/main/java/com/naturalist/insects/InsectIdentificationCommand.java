package com.naturalist.insects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.data.FileName;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import com.naturalist.vision.Image;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates the full insect identification flow: vision identification →
 * aggregate construction → transactional persistence. Replaces the three-step
 * controller orchestration (identify → ensureParentRanks → insert entities)
 * with a single command call.
 *
 * <p>Owns the tool schema, system prompt, and result deserialization for
 * vision identification. The identify-insect Claude Code skill has working
 * prompts that informed this implementation.
 *
 * <p>Returns {@link InsectSpeciesName} — a pragmatic CQS exception so the
 * caller can redirect to the species page without a follow-up query.
 */
public class InsectIdentificationCommand {

    private static final String TOOL_NAME = "propose_insect_species";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VisionService visionService;
    private final InsectCatalogIdentificationTransaction transaction;

    public InsectIdentificationCommand(VisionService visionService,
                                        InsectCatalogIdentificationTransaction transaction) {
        this.visionService = visionService;
        this.transaction = transaction;
    }

    /**
     * Identifies an insect from a photograph and persists the result —
     * species, parent ranks, image, and field observation — atomically.
     *
     * @param image          the photograph with metadata (location, captured instant)
     * @param storedFileName the already-stored image file name (infrastructure concern)
     * @param naturalist     the naturalist who captured the image
     * @param notes          optional field notes from the naturalist
     * @return the species name for redirect
     */
    public InsectSpeciesName identify(Image image, FileName storedFileName,
                                      NaturalistName naturalist,
                                      @Nullable String notes) {
        var result = identifyViaVision(image);

        var observationId = FieldObservationId.create();
        var capturedAt = image.metadata().capturedAt() != null
                ? image.metadata().capturedAt() : Instant.now();

        var rankName = result.identifiedEntity().rankName();

        var insectImage = new InsectImage(
                InsectImageId.create(),
                rankName,
                Instant.now(),
                storedFileName,
                observationId);

        var observation = new FieldObservation(
                observationId,
                naturalist,
                rankName,
                capturedAt,
                (notes == null || notes.isBlank()) ? null : notes,
                image.metadata().location(),
                result.identification());

        var catalogId = new CatalogIdentification(
                result.identifiedEntity(), result.taxonomy(), insectImage, observation,
                List.of(), List.of(), Map.of());

        transaction.execute(catalogId);

        var species = ((IdentifiedRankEntity.Species) result.identifiedEntity()).species();
        return species.name();
    }

    // ----- vision identification (unchanged from InsectIdentificationService) -----

    private InsectIdentificationResult identifyViaVision(Image image) {
        var toolSchema = buildToolSchema();
        var systemPrompt = buildSystemPrompt(image.metadata().location());
        var result = visionService.identify(image, toolSchema, systemPrompt);
        return parseResult(result);
    }

    private ToolSchema buildToolSchema() {
        var schema = """
                {
                  "type": "object",
                  "required": ["name", "identifiedRank", "order", "family", "commonName",
                               "descriptionPreschool", "descriptionElementary",
                               "descriptionSecondary", "descriptionUniversity",
                               "guilds", "beneficial", "confidence", "evidence", "features"],
                  "properties": {
                    "name":                    { "type": "string", "description": "Kebab-case slug for the identified rank, e.g. battus-philenor for species, syrphidae for family" },
                    "identifiedRank":          { "type": "string", "enum": ["ORDER", "FAMILY", "GENUS", "SPECIES"], "description": "The most specific Linnaean rank you can confidently identify" },
                    "order":                   { "type": "string", "description": "Taxonomic order, e.g. Lepidoptera" },
                    "family":                  { "type": "string", "description": "Taxonomic family, e.g. Papilionidae. Required for FAMILY, GENUS, and SPECIES ranks." },
                    "genus":                   { "type": ["string", "null"], "description": "Taxonomic genus, e.g. Battus. Required for GENUS and SPECIES ranks, null otherwise." },
                    "species":                 { "type": ["string", "null"], "description": "Species epithet, e.g. philenor. Required for SPECIES rank, null otherwise." },
                    "commonName":              { "type": "string", "description": "Most widely used common name for the identified rank" },
                    "descriptionPreschool":    { "type": "string", "description": "Durrell preschool-level description (simple, sensory, wonder-focused)" },
                    "descriptionElementary":   { "type": "string", "description": "Durrell elementary-level description (observable features, life cycle basics)" },
                    "descriptionSecondary":    { "type": "string", "description": "Durrell secondary-level description (ecology, adaptations, relationships)" },
                    "descriptionUniversity":   { "type": "string", "description": "Durrell university-level description (taxonomy, research context, conservation)" },
                    "guilds":                  { "type": "array", "items": { "type": "string", "enum": ["PARASITOID","PREDATOR","APEX_PREDATOR","POLLINATOR","DECOMPOSER","FOOD_WEB","MIGRATORY","KEYSTONE"] }, "description": "Functional ecological guilds" },
                    "beneficial":              { "type": "boolean", "description": "Whether this insect is beneficial in a garden/agricultural context" },
                    "features":                { "type": "array", "items": { "type": "string" }, "description": "Morphological field marks observed, ordered conspicuous to diagnostic: wing shape, coloration, antennae type, mouthparts, body segmentation, etc." },
                    "sightingNotes":           { "type": ["string", "null"], "description": "Notable observations about this sighting" },
                    "confidence":              { "type": "number", "minimum": 0, "maximum": 1, "description": "Confidence in identification (0.0-1.0)" },
                    "evidence":                { "type": "string", "description": "Which visible features support this identification" },
                    "alternatives":            { "type": ["string", "null"], "description": "JSON array of alternative candidates with name and confidence, or null if highly confident" }
                  }
                }
                """;
        return new ToolSchema(TOOL_NAME,
                "Propose an insect identification based on the provided photograph.",
                schema);
    }

    private String buildSystemPrompt(String location) {
        var prompt = """
                You are an expert entomologist assisting a naturalist in identifying insects
                from photographs. For each identification:

                1. Examine the photograph carefully, noting morphological features (wing
                   venation, body shape, coloration, antennae, leg structure).
                2. Consider the geographic location if provided — use it to narrow range maps
                   and eliminate look-alike species from other regions.
                3. Identify to the MOST SPECIFIC Linnaean rank your confidence supports:
                   - SPECIES: you can confidently name the species (e.g. Battus philenor)
                   - GENUS: you can identify the genus but not the species
                   - FAMILY: you can identify the family but not the genus
                   - ORDER: you can only identify the order
                   Set identifiedRank accordingly. Only provide genus/species fields when
                   your identifiedRank includes them. Do NOT guess a species if you are
                   not confident — identify at family or order level instead.
                4. Provide four Durrell-level descriptions for the identified rank:
                   - Preschool: simple, sensory, wonder-focused (what a 4-year-old would notice)
                   - Elementary: observable features, life cycle basics (what a 10-year-old learns)
                   - Secondary: ecology, adaptations, relationships (high school biology level)
                   - University: taxonomy, research context, conservation status (expert level)
                5. List the morphological features you observed in the photo, ordered from
                   most conspicuous to most diagnostic. These should be specific, normalised
                   field marks (e.g. "halteres", "clubbed antennae", "elytra").
                6. Assess your confidence honestly. Below 0.7, name the specific features you
                   cannot confirm from the photo.
                7. List alternative candidates if confidence is below 0.9.
                8. Assign functional ecological guilds from the allowed list.

                Generate the kebab-case slug name from the identified rank's name
                (e.g. battus-philenor for a species, syrphidae for a family).
                Use the propose_insect_species tool to return your identification.
                """;
        if (location != null && !location.isBlank()) {
            prompt += "\nLocation context: " + location;
        }
        return prompt;
    }

    private InsectIdentificationResult parseResult(ToolResult result) {
        try {
            var node = MAPPER.readTree(result.argumentsJson());
            var identifiedRank = node.get("identifiedRank").asText();
            var slug = node.get("name").asText();
            var description = new Description(
                    node.get("descriptionPreschool").asText(),
                    node.get("descriptionElementary").asText(),
                    node.get("descriptionSecondary").asText(),
                    node.get("descriptionUniversity").asText());
            var commonName = node.get("commonName").asText();
            var sightingNotes = node.hasNonNull("sightingNotes")
                    ? node.get("sightingNotes").asText() : null;

            var taxonomicOrder = TaxonomicOrder.of(node.get("order").asText());
            var taxonomicFamily = node.hasNonNull("family")
                    ? TaxonomicFamily.of(node.get("family").asText()) : null;
            var taxonomicGenus = node.hasNonNull("genus")
                    ? TaxonomicGenus.of(node.get("genus").asText()) : null;
            var taxonomicSpecies = node.hasNonNull("species")
                    ? TaxonomicSpecies.of(node.get("species").asText()) : null;
            var taxonomy = new TaxonomicClassification(
                    taxonomicOrder, taxonomicFamily, taxonomicGenus, taxonomicSpecies);

            var identifiedEntity = buildIdentifiedEntity(
                    identifiedRank, slug, description, commonName, sightingNotes, taxonomy);

            var features = new java.util.ArrayList<String>();
            if (node.hasNonNull("features") && node.get("features").isArray()) {
                for (var f : node.get("features")) {
                    if (f.isTextual() && !f.asText().isBlank()) {
                        features.add(f.asText());
                    }
                }
            }

            var confidence = node.get("confidence").asDouble();
            var evidence = node.get("evidence").asText();
            var identification = new Identification(confidence, evidence, parseAlternatives(node));

            return new InsectIdentificationResult(identifiedEntity, taxonomy, identification,
                    List.copyOf(features));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vision identification result", e);
        }
    }

    private IdentifiedRankEntity buildIdentifiedEntity(
            String rank, String slug, Description description, String commonName,
            @Nullable String sightingNotes, TaxonomicClassification taxonomy) {
        return switch (rank) {
            case "SPECIES" -> {
                var speciesName = InsectSpeciesName.of(slug);
                var genusName = InsectGenusName.of(
                        taxonomy.genus().value().toLowerCase(java.util.Locale.ROOT));
                yield new IdentifiedRankEntity.Species(new InsectSpecies(
                        speciesName, genusName, taxonomy.species(), description,
                        Set.of(CommonName.of(commonName)), sightingNotes,
                        null, null, null, null, null, null, null, null));
            }
            case "GENUS" -> {
                var genusName = InsectGenusName.of(slug);
                var familyName = InsectFamilyName.of(
                        taxonomy.family().value().toLowerCase(java.util.Locale.ROOT));
                yield new IdentifiedRankEntity.Genus(new InsectGenus(
                        genusName, familyName, taxonomy.genus(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            case "FAMILY" -> {
                var familyName = InsectFamilyName.of(slug);
                var orderName = InsectOrderName.of(
                        taxonomy.order().value().toLowerCase(java.util.Locale.ROOT));
                yield new IdentifiedRankEntity.Family(new InsectFamily(
                        familyName, orderName, taxonomy.family(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            case "ORDER" -> {
                var orderName = InsectOrderName.of(slug);
                yield new IdentifiedRankEntity.Order(new InsectOrder(
                        orderName, taxonomy.order(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            default -> throw new IllegalArgumentException(
                    "Unknown identified rank: " + rank);
        };
    }

    private List<Identification.Candidate> parseAlternatives(JsonNode node) {
        if (!node.hasNonNull("alternatives")) {
            return List.of();
        }
        var alt = node.get("alternatives");
        JsonNode array;
        try {
            array = alt.isTextual() ? MAPPER.readTree(alt.asText()) : alt;
        } catch (Exception e) {
            return List.of();
        }
        if (array == null || !array.isArray()) {
            return List.of();
        }
        var candidates = new ArrayList<Identification.Candidate>();
        for (var candidate : array) {
            var scientificName = candidate.hasNonNull("name") ? candidate.get("name").asText() : null;
            if (scientificName == null || scientificName.isBlank()) {
                continue;
            }
            var commonName = candidate.hasNonNull("commonName") ? candidate.get("commonName").asText() : null;
            var candidateConfidence = candidate.hasNonNull("confidence") ? candidate.get("confidence").asDouble() : 0.0;
            candidates.add(new Identification.Candidate(scientificName, commonName, candidateConfidence));
        }
        return List.copyOf(candidates);
    }
}
