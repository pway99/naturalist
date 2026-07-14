package com.naturalist.insects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import com.naturalist.vision.Image;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates vision identification for insects. Owns the tool schema,
 * system prompt, and result deserialization. The identify-insect Claude Code
 * skill has working prompts that informed this implementation.
 */
@DomainService
public class InsectIdentificationService {

    private static final String TOOL_NAME = "propose_insect_species";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VisionService visionService;

    public InsectIdentificationService(VisionService visionService) {
        this.visionService = visionService;
    }

    public InsectIdentificationResult identify(Image image) {
        var toolSchema = buildToolSchema();
        var systemPrompt = buildSystemPrompt(image.metadata().location());
        var result = visionService.identify(image, toolSchema, systemPrompt);
        return parseResult(result);
    }

    private ToolSchema buildToolSchema() {
        var schema = """
                {
                  "type": "object",
                  "required": ["name", "order", "family", "genus", "species", "commonName",
                               "descriptionPreschool", "descriptionElementary",
                               "descriptionSecondary", "descriptionUniversity",
                               "guilds", "beneficial", "confidence", "evidence"],
                  "properties": {
                    "name":                    { "type": "string", "description": "Kebab-case slug for the species, e.g. battus-philenor" },
                    "order":                   { "type": "string", "description": "Taxonomic order, e.g. Lepidoptera" },
                    "family":                  { "type": "string", "description": "Taxonomic family, e.g. Papilionidae" },
                    "genus":                   { "type": "string", "description": "Taxonomic genus, e.g. Battus" },
                    "species":                 { "type": "string", "description": "Species epithet, e.g. philenor" },
                    "commonName":              { "type": "string", "description": "Most widely used common name" },
                    "descriptionPreschool":    { "type": "string", "description": "Durrell preschool-level description (simple, sensory, wonder-focused)" },
                    "descriptionElementary":   { "type": "string", "description": "Durrell elementary-level description (observable features, life cycle basics)" },
                    "descriptionSecondary":    { "type": "string", "description": "Durrell secondary-level description (ecology, adaptations, relationships)" },
                    "descriptionUniversity":   { "type": "string", "description": "Durrell university-level description (taxonomy, research context, conservation)" },
                    "guilds":                  { "type": "array", "items": { "type": "string", "enum": ["PARASITOID","PREDATOR","APEX_PREDATOR","POLLINATOR","DECOMPOSER","FOOD_WEB","MIGRATORY","KEYSTONE"] }, "description": "Functional ecological guilds" },
                    "beneficial":              { "type": "boolean", "description": "Whether this insect is beneficial in a garden/agricultural context" },
                    "sightingNotes":           { "type": ["string", "null"], "description": "Notable observations about this sighting" },
                    "confidence":              { "type": "number", "minimum": 0, "maximum": 1, "description": "Confidence in identification (0.0-1.0)" },
                    "evidence":                { "type": "string", "description": "Which visible features support this identification" },
                    "alternatives":            { "type": ["string", "null"], "description": "JSON array of alternative candidates with name and confidence, or null if highly confident" }
                  }
                }
                """;
        return new ToolSchema(TOOL_NAME,
                "Propose an insect species identification based on the provided photograph.",
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
                3. Provide four Durrell-level descriptions:
                   - Preschool: simple, sensory, wonder-focused (what a 4-year-old would notice)
                   - Elementary: observable features, life cycle basics (what a 10-year-old learns)
                   - Secondary: ecology, adaptations, relationships (high school biology level)
                   - University: taxonomy, research context, conservation status (expert level)
                4. Assess your confidence honestly. Below 0.7, name the specific features you
                   cannot confirm from the photo.
                5. List alternative candidates if confidence is below 0.9.
                6. Assign functional ecological guilds from the allowed list.

                Generate the kebab-case slug name from the binomial name (e.g. battus-philenor).
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
            var name = InsectSpeciesName.of(node.get("name").asText());
            var genusSlug = node.get("genus").asText().toLowerCase();
            var genusName = InsectGenusName.of(genusSlug);
            var taxonomy = new TaxonomicClassification(
                    TaxonomicOrder.of(node.get("order").asText()),
                    TaxonomicFamily.of(node.get("family").asText()),
                    TaxonomicGenus.of(node.get("genus").asText()),
                    TaxonomicSpecies.of(node.get("species").asText()));
            var description = new Description(
                    node.get("descriptionPreschool").asText(),
                    node.get("descriptionElementary").asText(),
                    node.get("descriptionSecondary").asText(),
                    node.get("descriptionUniversity").asText());
            var commonName = node.get("commonName").asText();

            // Build the InsectSpecies — use the current 14-component constructor shape.
            // Nullable value-object fields (chemicalDefense, voltinism, habitatProfile,
            // habitatRequirements, gardenConnections, beneficialProfile, ecologicalSignificance)
            // are null; they are populated incrementally by the naturalist.
            var species = new InsectSpecies(
                    name,
                    genusName,
                    taxonomy.species(),
                    description,
                    Set.of(CommonName.of(commonName)),
                    node.has("sightingNotes") && !node.get("sightingNotes").isNull()
                            ? node.get("sightingNotes").asText() : null,
                    null,  // placedIn (Clade) — not vision-determinable
                    null,  // chemicalDefense
                    null,  // voltinism
                    null,  // habitatProfile
                    null,  // habitatRequirements
                    null,  // gardenConnections
                    null,  // beneficialProfile
                    null   // ecologicalSignificance
            );

            var confidence = node.get("confidence").asDouble();
            var evidence = node.get("evidence").asText();
            var identification = new Identification(confidence, evidence, parseAlternatives(node));

            return new InsectIdentificationResult(species, taxonomy, identification);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vision identification result", e);
        }
    }

    /**
     * Parses the tool's {@code alternatives} field into typed candidates. The
     * schema declares it as a JSON-array string (or null), but a model may also
     * return it as an array node directly — handle both, and treat anything
     * unparseable or missing as no alternatives rather than failing the whole
     * identification.
     */
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
