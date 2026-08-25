package com.naturalist.insects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.authority.OnlineSource;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.data.FileName;
import com.naturalist.featuresearch.FeatureSearch;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.library.CitationAttribution;
import com.naturalist.library.LibraryCommand;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import com.naturalist.textgeneration.TextGenerationService;
import com.naturalist.vision.Image;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.observation.Identification;

/**
 * Orchestrates the full insect identification flow: vision identification
 * → authority enrichment (best-effort) → parent rank enrichment → feature
 * resolution → citation creation → transactional persistence. All external
 * calls (vision, authority lookup, content fetch, text generation) complete
 * before the transaction boundary. The transaction does pure DB writes.
 *
 * <p>Authority lookups enrich the identification with citations and
 * grounded descriptions but never gate it — the vision result is
 * authoritative for rank determination.
 */
class InsectIdentificationCommand {

    private static final String TOOL_NAME = "propose_insect_species";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AuthoritySource VISION_SUGGESTED =
            new AuthoritySource("ref", "Suggested Reference");

    private final VisionService visionService;
    private final TextGenerationService textGenerationService;
    private final ExternalAuthority externalAuthority;
    private final LibraryCommand libraryCommand;
    private final InsectQuery insectQuery;
    // Wired but not yet used -- reuse-aware dedup lands in the next task.
    private final FeatureSearch<InsectFeatureId> featureSearch;
    private final InsectCatalogIdentificationTransaction transaction;

    InsectIdentificationCommand(VisionService visionService,
                                        TextGenerationService textGenerationService,
                                        ExternalAuthority externalAuthority,
                                        LibraryCommand libraryCommand,
                                        InsectQuery insectQuery,
                                        FeatureSearch<InsectFeatureId> featureSearch,
                                        InsectCatalogIdentificationTransaction transaction) {
        this.visionService = visionService;
        this.textGenerationService = textGenerationService;
        this.externalAuthority = externalAuthority;
        this.libraryCommand = libraryCommand;
        this.insectQuery = insectQuery;
        this.featureSearch = featureSearch;
        this.transaction = transaction;
    }

    /**
     * Identifies an insect from a photograph, enriches with authority
     * content and parent rank descriptions, resolves features, creates
     * citations, and persists everything atomically.
     *
     * <p>Authority lookups are best-effort enrichment — the vision result
     * determines the identified rank. All external calls complete before
     * the transaction boundary.
     *
     * @return the identified rank name for redirect
     */
    public InsectRankName identify(Image image, FileName storedFileName,
                                    NaturalistName naturalist,
                                    @Nullable String notes) {
        // 1. VISION -- external call
        var visionResult = identifyViaVision(image);
        var identifiedEntity = visionResult.identifiedEntity();
        var taxonomy = visionResult.taxonomy();
        var rankName = identifiedEntity.rankName();

        // 2. AUTHORITY ENRICHMENT -- best-effort, never gates
        var authorityRefs = collectAuthorityRefs(rankName, taxonomy);
        if (visionResult.referenceUrl() != null) {
            authorityRefs.putIfAbsent(rankName, Set.of(
                    new AuthorityReference(VISION_SUGGESTED, visionResult.referenceUrl())));
        }

        // 3. PARENT RANK ENRICHMENT -- external calls, only for new ranks
        var parentDescriptions = enrichParentRanks(taxonomy);
        var parentFeatures = new ArrayList<RankFeatures>();
        parentDescriptions.forEach((rn, enrichment) ->
                parentFeatures.add(new RankFeatures(rn, enrichment.features())));

        // 4. FEATURE RESOLUTION -- queries only
        var identifiedRankFeatures = new RankFeatures(rankName, visionResult.features());
        var combined = new ArrayList<RankFeatures>();
        combined.add(identifiedRankFeatures);
        combined.addAll(parentFeatures);
        var featureResolution = resolveFeatures(combined);

        // 5. CITATION PREPARATION + LIBRARY WRITES -- cross-domain, best-effort
        writeCitations(authorityRefs);

        // 6. INSECT TRANSACTION -- pure DB writes
        var observationId = InsectObservationId.create();
        var capturedAt = image.metadata().capturedAt() != null
                ? image.metadata().capturedAt() : Instant.now();

        var insectImage = new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(), rankName, Instant.now(),
                storedFileName, observationId);

        var observation = new OrganismObservation<InsectObservationId, InsectRankName>(
                observationId, naturalist, rankName, capturedAt,
                (notes == null || notes.isBlank()) ? null : notes,
                image.metadata().location(),
                visionResult.identification());

        var descriptionMap = new HashMap<InsectRankName, Description>();
        parentDescriptions.forEach((name, enrichment) ->
                descriptionMap.put(name, enrichment.description()));

        var catalogId = new CatalogIdentification(
                identifiedEntity, taxonomy,
                insectImage, observation,
                featureResolution.newFeatures(), featureResolution.assignments(),
                descriptionMap);

        transaction.execute(catalogId);

        return rankName;
    }

    // ----- vision identification -----

    private InsectIdentificationResult identifyViaVision(Image image) {
        var toolSchema = buildToolSchema();
        var systemPrompt = buildSystemPrompt(image.metadata().location());
        var exchange = visionService.identify(image, toolSchema, systemPrompt);
        return parseResult(exchange.result());
    }

    // ----- authority enrichment (best-effort) -----

    private Map<InsectRankName, Set<AuthorityReference>> collectAuthorityRefs(
            InsectRankName identifiedRank, TaxonomicClassification taxonomy) {
        var refs = new LinkedHashMap<InsectRankName, Set<AuthorityReference>>();

        tryCollectRef(identifiedRank, refs);

        if (identifiedRank instanceof InsectSpeciesName && taxonomy.genus() != null) {
            tryCollectRef(InsectGenusName.of(
                    taxonomy.genus().value().toLowerCase(Locale.ROOT)), refs);
        }
        if ((identifiedRank instanceof InsectSpeciesName
                || identifiedRank instanceof InsectGenusName)
                && taxonomy.family() != null) {
            var familyName = InsectFamilyName.of(
                    taxonomy.family().value().toLowerCase(Locale.ROOT));
            if (!refs.containsKey(familyName)) tryCollectRef(familyName, refs);
        }
        if (!(identifiedRank instanceof InsectOrderName)) {
            var orderName = InsectOrderName.of(
                    taxonomy.order().value().toLowerCase(Locale.ROOT));
            if (!refs.containsKey(orderName)) tryCollectRef(orderName, refs);
        }

        return refs;
    }

    private void tryCollectRef(InsectRankName rankName,
                               Map<InsectRankName, Set<AuthorityReference>> refs) {
        try {
            var found = externalAuthority.lookup((EntityName) rankName);
            if (!found.isEmpty()) refs.put(rankName, found);
        } catch (Exception e) {
            // best-effort — identification proceeds without this reference
        }
    }

    private static final Description FALLBACK_DESCRIPTION = new Description(
            "Identified via vision -- description pending.",
            "Identified via vision -- description pending.",
            "Identified via vision -- description pending.",
            "Identified via vision -- description pending.");

    // ----- parent rank enrichment -----

    private record RankEnrichment(Description description, List<String> features) {}

    private Map<InsectRankName, RankEnrichment> enrichParentRanks(
            TaxonomicClassification taxonomy) {
        var enrichments = new LinkedHashMap<InsectRankName, RankEnrichment>();

        // Check each parent rank -- only enrich if it doesn't already exist
        if (taxonomy.genus() != null) {
            var genusName = InsectGenusName.of(
                    taxonomy.genus().value().toLowerCase(Locale.ROOT));
            enrichIfNew(genusName, taxonomy.genus().value(), enrichments);
        }
        if (taxonomy.family() != null) {
            var familyName = InsectFamilyName.of(
                    taxonomy.family().value().toLowerCase(Locale.ROOT));
            enrichIfNew(familyName, taxonomy.family().value(), enrichments);
        }
        var orderName = InsectOrderName.of(
                taxonomy.order().value().toLowerCase(Locale.ROOT));
        enrichIfNew(orderName, taxonomy.order().value(), enrichments);

        return enrichments;
    }

    private void enrichIfNew(InsectRankName rankName, String displayName,
                             Map<InsectRankName, RankEnrichment> enrichments) {
        // Check existence by querying the appropriate rank
        boolean exists = switch (rankName) {
            case InsectOrderName n -> insectQuery.orders().getByName(n).isPresent();
            case InsectFamilyName n -> insectQuery.families().getByName(n).isPresent();
            case InsectGenusName n -> insectQuery.genera().getByName(n).isPresent();
            default -> true; // species/subspecies not parent ranks
        };
        if (exists) return;

        try {
            var refs = externalAuthority.lookup((EntityName) rankName);
            if (refs.isEmpty()) {
                enrichments.put(rankName, new RankEnrichment(FALLBACK_DESCRIPTION, List.of()));
                return;
            }
            var ref = refs.iterator().next();
            var content = externalAuthority.fetchContent(ref);
            var enrichment = generateEnrichment(displayName, content);
            enrichments.put(rankName, enrichment);
        } catch (Exception e) {
            enrichments.put(rankName, new RankEnrichment(FALLBACK_DESCRIPTION, List.of()));
        }
    }

    private RankEnrichment generateEnrichment(String taxonName, AuthorityContent content) {
        var schema = """
                {
                  "type": "object",
                  "required": ["descriptionPreschool", "descriptionElementary",
                               "descriptionSecondary", "descriptionUniversity", "features"],
                  "properties": {
                    "descriptionPreschool":    { "type": "string", "description": "Durrell preschool-level description" },
                    "descriptionElementary":   { "type": "string", "description": "Durrell elementary-level description" },
                    "descriptionSecondary":    { "type": "string", "description": "Durrell secondary-level description" },
                    "descriptionUniversity":   { "type": "string", "description": "Durrell university-level description" },
                    "features":                { "type": "array", "items": { "type": "string" }, "description": "Diagnostic morphological features for this rank, ordered conspicuous to diagnostic" }
                  }
                }
                """;
        var tool = new ToolSchema("describe_taxon",
                "Generate Durrell descriptions and diagnostic features for a taxon.", schema);
        var systemPrompt = """
                You are an expert entomologist. Given authoritative reference content about
                an insect taxon, produce four Durrell-level descriptions grounded in the
                provided content. Do not invent facts -- only reshape what the source says.
                Also extract the diagnostic morphological features that characterise this
                taxon, ordered from most conspicuous to most diagnostic.
                """;
        var userPrompt = "Taxon: " + taxonName + "\n\nAuthority content:\n" + content.content();

        var result = textGenerationService.generate(tool, systemPrompt, userPrompt);
        try {
            var node = MAPPER.readTree(result.argumentsJson());
            var description = new Description(
                    node.get("descriptionPreschool").asText(),
                    node.get("descriptionElementary").asText(),
                    node.get("descriptionSecondary").asText(),
                    node.get("descriptionUniversity").asText());
            var features = new ArrayList<String>();
            if (node.hasNonNull("features") && node.get("features").isArray()) {
                for (var f : node.get("features")) {
                    if (f.isTextual() && !f.asText().isBlank()) {
                        features.add(f.asText());
                    }
                }
            }
            return new RankEnrichment(description, List.copyOf(features));
        } catch (Exception e) {
            return new RankEnrichment(FALLBACK_DESCRIPTION, List.of());
        }
    }

    // ----- feature resolution -----

    private record RankFeatures(InsectRankName rankName, List<String> featureValues) {}

    private record FeatureResolution(
            List<InsectFeature> newFeatures,
            List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> assignments
    ) {}

    private FeatureResolution resolveFeatures(List<RankFeatures> allRankFeatures) {
        var newFeatures = new ArrayList<InsectFeature>();
        var assignments = new ArrayList<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>();
        // Track features we've already created in this invocation
        var createdFeatures = new HashMap<String, InsectFeatureId>();

        for (var rankFeatures : allRankFeatures) {
            int ordinal = 0;
            for (var value : rankFeatures.featureValues()) {
                var normalized = value.trim().toLowerCase();
                if (normalized.isBlank()) continue;

                InsectFeatureId featureId;
                if (createdFeatures.containsKey(normalized)) {
                    featureId = createdFeatures.get(normalized);
                } else {
                    featureId = InsectFeatureId.create();
                    newFeatures.add(InsectFeature.of(featureId, normalized));
                    createdFeatures.put(normalized, featureId);
                }

                assignments.add(OrganismFeatureAssignment.of(
                        InsectFeatureAssignmentId.create(),
                        featureId, rankFeatures.rankName(), ordinal++));
            }
        }

        return new FeatureResolution(List.copyOf(newFeatures), List.copyOf(assignments));
    }

    // ----- citation creation -----

    private static final InsectsDomain INSECTS_DOMAIN = new InsectsDomain();

    private void writeCitations(Map<InsectRankName, Set<AuthorityReference>> authorityRefs) {
        for (var entry : authorityRefs.entrySet()) {
            var rankName = entry.getKey();
            for (var ref : entry.getValue()) {
                try {
                    var citationSlug = ref.source().id() + "-" + rankName.value();
                    var citationName = CitationName.of(citationSlug);
                    var title = capitalize(rankName.value()) + " -- " + ref.source().displayName();
                    var citation = new OnlineSource(
                            citationName, ref, title, null, null, null);
                    var attribution = new CitationAttribution(
                            citation,
                            new EntityRef(INSECTS_DOMAIN, (EntityName) rankName),
                            "Identified via vision");
                    // Idempotency for both the citation and its association to the
                    // rank is the library domain's own responsibility (see
                    // CitationAttributionTransaction) -- no constraint exception is
                    // expected here, so none is caught.
                    libraryCommand.attributeCitation(attribution);
                } catch (Exception e) {
                    // Degraded -- deliberately still swallowed here, but for a
                    // different reason than before: attributeCitation runs as its
                    // own library-owned transaction that commits or rolls back
                    // independently, and the insect transaction below has not
                    // started yet. A genuine failure in the library write leaves
                    // the identification to proceed without this citation rather
                    // than aborting or corrupting anything -- this is the one place
                    // in this method where swallowing is defensible.
                }
            }
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ----- tool schema + parsing (unchanged from Task 6) -----

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
                    "alternatives":            { "type": ["string", "null"], "description": "JSON array of alternative candidates with name and confidence, or null if highly confident" },
                    "referenceUrl":            { "type": ["string", "null"], "description": "URL to the most relevant authoritative reference page for this taxon (e.g. https://eol.org/pages/7467 for Carabidae). Suggest the single best page from EOL, iNaturalist, or Wikipedia. Only include URLs with predictable structure — avoid sites with opaque numeric IDs." }
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
                2. Consider the geographic location if provided -- use it to narrow range maps
                   and eliminate look-alike species from other regions.
                3. Identify to the MOST SPECIFIC Linnaean rank your confidence supports:
                   - SPECIES: you can confidently name the species (e.g. Battus philenor)
                   - GENUS: you can identify the genus but not the species
                   - FAMILY: you can identify the family but not the genus
                   - ORDER: you can only identify the order
                   Set identifiedRank accordingly. Only provide genus/species fields when
                   your identifiedRank includes them. Do NOT guess a species if you are
                   not confident -- identify at family or order level instead.
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
                9. If you know the URL to an authoritative reference page for this taxon
                   (e.g. Encyclopedia of Life, iNaturalist, Wikipedia, bugguide.net), include it as
                   referenceUrl. Only include URLs you are confident are correct — do
                   not guess numeric page IDs.

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

            var features = new ArrayList<String>();
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

            URI referenceUrl = null;
            if (node.hasNonNull("referenceUrl") && !node.get("referenceUrl").asText().isBlank()) {
                try {
                    referenceUrl = URI.create(node.get("referenceUrl").asText());
                } catch (IllegalArgumentException ignored) {
                    // invalid URL — skip
                }
            }

            return new InsectIdentificationResult(identifiedEntity, taxonomy, identification,
                    List.copyOf(features), referenceUrl);
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
                        taxonomy.genus().value().toLowerCase(Locale.ROOT));
                yield new IdentifiedRankEntity.Species(new InsectSpecies(
                        speciesName, genusName, taxonomy.species(), description,
                        Set.of(CommonName.of(commonName)), sightingNotes,
                        null, null, null, null, null, null, null, null));
            }
            case "GENUS" -> {
                var genusName = InsectGenusName.of(slug);
                var familyName = InsectFamilyName.of(
                        taxonomy.family().value().toLowerCase(Locale.ROOT));
                yield new IdentifiedRankEntity.Genus(new InsectGenus(
                        genusName, familyName, taxonomy.genus(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            case "FAMILY" -> {
                var familyName = InsectFamilyName.of(slug);
                var orderName = InsectOrderName.of(
                        taxonomy.order().value().toLowerCase(Locale.ROOT));
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
            var candidateCommonName = candidate.hasNonNull("commonName") ? candidate.get("commonName").asText() : null;
            var candidateConfidence = candidate.hasNonNull("confidence") ? candidate.get("confidence").asDouble() : 0.0;
            candidates.add(new Identification.Candidate(scientificName, candidateCommonName, candidateConfidence));
        }
        return List.copyOf(candidates);
    }
}
