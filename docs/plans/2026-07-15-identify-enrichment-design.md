# Insect Identification Enrichment Design

## Problem

`InsectsController#identify` and `InsectIdentificationCommand#identify` ship a functional
vision-based identification flow but have five gaps:

1. **Species-only assumption** — the tool schema, `CatalogIdentification` aggregate, and
   return type all assume species-level identification. If the vision model can only
   confidently identify to family or order, it is forced to guess a species, producing
   inaccurate catalog entries.
2. **No authority validation** — hallucinated taxon names pass unchecked into the catalog.
   There is no cross-reference against an external taxonomic authority.
3. **No citations** — the `ExternalAuthority`/`Citation`/`InsectCitationView` system exists
   on the read side but is not wired into the identification flow. Identified ranks have
   no provenance link to an authoritative source.
4. **Features not captured** — `InsectFeature`, `InsectFeatureAssignment`, and
   `InsectFeatureView` exist with full repository and query stacks, but the identification
   flow only captures "evidence" as a narrative string. Structured field marks are not
   persisted.
5. **Parent rank PLACEHOLDER descriptions** — when parent ranks (order, family, genus)
   are created during identification, they receive identical placeholder text instead of
   real four-level Durrell descriptions. Parent rank features are also absent.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Rank polymorphism | Vision may identify to order, family, genus, or species | Forcing species-level answers produces inaccurate catalog entries |
| Authority validation | `ExternalAuthority.lookup()` validates each rank; fallback up the chain | Hallucination guard — if species lookup fails but family succeeds, catalog at family level |
| Identification rejection | If all ranks fail authority validation, the identification is rejected | A rank with no authority backing should not enter the catalog |
| Citation creation | Create `Citation` + `CitationAssociation` via library domain for every confirmed rank | Gives the naturalist a clickable link to cross-reference accuracy |
| Citation idempotency | Owned by the library domain, not the insects command | The library owns its own consistency rules |
| Feature extraction | Vision returns features as a structured array alongside the narrative evidence | The model is already reasoning about features; structured output costs nothing |
| Parent rank descriptions | Generated from authority content via `TextGenerationService` | Descriptions should be grounded in what the authority actually says, not AI training data |
| Parent rank features | Extracted from authority content alongside descriptions | If a parent rank is new, its diagnostic field marks should also be captured |
| Description generation trigger | Only when the parent rank does not already exist | Avoids wasted generation for ranks already in the catalog |
| Text generation port | New `TextGenerationService` kernel, separate from `VisionService` | Semantically distinct concerns; text-only calls avoid re-sending the image |
| Authority content fetch | `fetchContent(AuthorityReference)` method on `ExternalAuthority` | The authority implementation knows its own page structure; co-locate with lookup |
| Transaction discipline | All external calls (vision, authority, text generation) complete before the transaction | The `@Transactional` boundary must not hold a DB connection during network I/O |
| Synchronous execution | Everything runs synchronously for now (CQS pattern) | Build the capability first; refactor to event-driven enrichment later |
| Orchestration location | All logic in `insects-core`; controller stays thin | Controller accepts input, calls command, redirects |

## New Kernel Infrastructure

### `kernels/text-generation/` — `TextGenerationService`

```java
package com.naturalist.textgeneration;

import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

public interface TextGenerationService {
    ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt);
}
```

Text-only generation port symmetric with `VisionService`. Takes a tool schema and
prompts, returns a `ToolResult`. The Anthropic adapter implements both ports using the
same SDK — different message shapes (no image block), same underlying client.

Depends on `kernels/vision/` for `ToolSchema` and `ToolResult` (shared vocabulary).

### `ExternalAuthority` expansion

```java
package com.naturalist.authority;

public interface ExternalAuthority {
    AuthoritySource source();
    Set<AuthorityReference> lookup(EntityName subject);
    AuthorityContent fetchContent(AuthorityReference ref);  // new
}
```

### `AuthorityContent` — new value object in `kernels/authority/`

Carries the textual content retrieved from an authority's page for a given reference.
The `TextGenerationService` uses this as grounded source material for Durrell description
generation.

```java
package com.naturalist.authority;

public record AuthorityContent(
        AuthorityReference reference,
        String content
) implements ValueObject { ... }
```

## Rank-Polymorphic Identification

### Vision Tool Schema Changes

The tool schema gains:
- `"identifiedRank"` — required enum: `"ORDER"`, `"FAMILY"`, `"GENUS"`, `"SPECIES"`.
  Tells the model to identify to the most specific rank its confidence supports.
- `"features"` — required array of strings. Morphological field marks ordered
  conspicuous to diagnostic.
- `"genus"` and `"species"` become conditionally required (only when
  `identifiedRank` is `"GENUS"` or `"SPECIES"` respectively).

The system prompt instructs: identify to the most specific rank your confidence
supports. If you can confidently say "this is a hoverfly (Syrphidae)" but not which
species, set `identifiedRank: "FAMILY"` and omit genus/species fields.

### `InsectIdentificationResult` Changes

Currently carries `InsectSpecies`, `TaxonomicClassification`, `Identification`.
Changes to carry:

- `InsectRankName identifiedRankName` — the polymorphic rank name (any permit)
- The identified rank entity (species, genus, family, or order)
- `TaxonomicClassification` — with nullable genus/species (already supported)
- `Identification` — confidence, evidence, alternatives (unchanged)
- `List<String> features` — structured field marks from vision

### `CatalogIdentification` Aggregate Changes

Generalizes from `InsectSpecies species` to a polymorphic identified rank. The
image `parentName` and observation `subject` point at the identified `InsectRankName`.
Cross-entity FK invariants update to validate against the identified rank name
rather than a species name.

### Return Type

`InsectIdentificationCommand#identify` returns `InsectRankName` instead of
`InsectSpeciesName`. The controller redirects to `/insects/{rankName.value()}` which
already resolves polymorphically via `InsectTaxonView`.

## Authority Validation with Rank Fallback

After vision returns its identification, the command validates each rank in the
Linnaean chain against the external authority before any persistence.

**Validation flow (top-down):**

1. Start at the identified rank (e.g., species "eristalis-tenax")
2. Call `externalAuthority.lookup(rankEntityName)`
3. If the authority returns references → confirmed. This is the catalog rank.
4. If empty → fall back one rank up (species → genus → family → order)
5. Repeat until a rank is confirmed or all ranks exhausted
6. If all ranks fail → reject identification, nothing persisted

The confirmed rank becomes the identified rank for all downstream processing —
features attach here, image and field observation reference this rank, descriptions
are generated for this rank's parents.

**Example:** Vision says species "eristalis-tenax" (family Syrphidae, order Diptera).
Authority confirms Syrphidae but returns nothing for "eristalis-tenax." The image is
cataloged at family level. The field observation subject is
`InsectFamilyName.of("syrphidae")`. The naturalist sees "Syrphidae" on their
collection page with a citation link to the authority's Syrphidae page.

## Feature Persistence

### Vision Features (identified rank)

The vision response `features` array is parsed into structured `InsectFeature` entities:

1. For each feature string, normalize (trim + lowercase — matching `InsectFeature`'s
   compact constructor)
2. Check if an `InsectFeature` with that value already exists (query, pre-transaction)
3. If not, create a new `InsectFeature(InsectFeatureId.create(), value)`
4. Create an `InsectFeatureAssignment` linking the feature to the confirmed rank,
   with ordinal matching the array position

### Parent Rank Features (new ranks only)

When a parent rank is newly created, the text generation call that produces its
Durrell description also extracts diagnostic features for that rank. The tool schema
for the text generation call includes a `features` array alongside the four
description fields.

Same normalization, idempotency checks, and `InsectFeatureAssignment` creation as
vision features, but assigned to the parent rank instead of the identified rank.

### `InsectCommand` Expansion

```java
FeatureCommand features();
FeatureAssignmentCommand featureAssignments();

interface FeatureCommand extends EntityCommand<InsectFeatureId, InsectFeature> {}
interface FeatureAssignmentCommand
        extends EntityCommand<InsectFeatureAssignmentId, InsectFeatureAssignment> {}
```

## Citations via the Library Domain

When a rank is confirmed by the authority, the command creates a `Citation` and
`CitationAssociation` through the library domain's write surface.

**For confirmed rank + each new parent rank:**

1. Authority lookup returned `Set<AuthorityReference>`
2. For each `AuthorityReference`, construct an `OnlineSource` citation:
   - `CitationName` — derived from authority source + rank name (e.g., `"eol-syrphidae"`)
   - `AuthorityReference` — from lookup
   - `title` — taxon name + authority source (e.g., "Syrphidae — Encyclopedia of Life")
3. Construct a `CitationAssociation` linking the citation to the rank via
   `EntityRef(insectsDomainId, rankName)`
4. Insert via library domain write surface — idempotency owned by library

**Cross-domain dependency:** `insects-core` depends on `library-api` (permitted by DAG).
The library domain's write surface is injected into `InsectIdentificationCommand`.

## Grounded Durrell Descriptions for New Parent Ranks

When a parent rank does not already exist in the catalog:

1. The authority has already been consulted — we have an `AuthorityReference`
2. Call `externalAuthority.fetchContent(ref)` → `AuthorityContent`
3. Call `textGenerationService.generate(toolSchema, systemPrompt, userPrompt)` with:
   - The authority content as source material in the user prompt
   - Instructions to reshape into four Durrell levels
   - The tool schema requesting four description fields + features array
4. Parse the response into a `Description` and a `List<String>` of features
5. Construct the rank entity with the real description

**Failure mode:** If `fetchContent` or text generation fails, the rank is created
with a PLACEHOLDER description and empty features. The identification is not blocked.

## Full Orchestration Flow

All orchestration lives in `InsectIdentificationCommand` in `insects-core`.
The controller stays thin: accept input, call command, redirect.

```
1. VISION (external)
   └─ visionService.identify(image, toolSchema, systemPrompt)
   └─ parse → identified rank entity, taxonomy, features[], identification

2. AUTHORITY VALIDATION (external, top-down fallback)
   └─ for rank in [identified, ...parents]:
       └─ externalAuthority.lookup(rankName)
       └─ first confirmed rank = catalog target
       └─ if all fail → reject identification
   └─ result: confirmed rank + AuthorityReferences for confirmed + parents

3. PARENT RANK ENRICHMENT (external, only for new ranks)
   └─ for each parent rank not already in catalog:
       └─ externalAuthority.fetchContent(ref) → AuthorityContent
       └─ textGenerationService.generate(...) → Description + features[]
       └─ fallback to PLACEHOLDER description + empty features on failure

4. FEATURE RESOLUTION (queries only)
   └─ for each feature from vision (identified rank) + text generation (parent ranks):
       └─ check if InsectFeature exists → collect new ones
       └─ build InsectFeatureAssignment list per rank

5. CITATION PREPARATION
   └─ for confirmed rank + each new parent rank:
       └─ build OnlineSource + CitationAssociation

6. LIBRARY WRITES (cross-domain, outside insect transaction)
   └─ library write surface handles insert + idempotency

7. INSECT TRANSACTION (pure DB writes)
   └─ resolve/create parent ranks (with real descriptions)
   └─ insert identified rank entity if new
   └─ insert image
   └─ insert field observation
   └─ insert new InsectFeature entities
   └─ insert InsectFeatureAssignment records for all ranks
```

## `InsectIdentificationCommand` Dependencies

| Dependency | Source | Purpose |
|------------|--------|---------|
| `VisionService` | `kernels/vision` (existing) | Image-based identification |
| `TextGenerationService` | `kernels/text-generation` (new) | Authority-grounded description + feature generation |
| `ExternalAuthority` | `kernels/authority` (expanded) | Rank validation, content fetch |
| Library write surface | `library-api` (cross-domain) | Citation + association persistence |
| `InsectQuery` | `insects-api` (existing) | Existence checks (pre-transaction) |
| `InsectCatalogIdentificationTransaction` | `insects-core` (widened) | Atomic persistence of insect-domain entities |

## Failure Semantics

| Failure | Outcome |
|---------|---------|
| Vision fails | Entire identification fails, nothing persisted |
| Authority confirms nothing at any rank | Identification rejected, nothing persisted |
| Content fetch fails for a parent rank | Parent created with PLACEHOLDER description, empty features |
| Text generation fails for a parent rank | Parent created with PLACEHOLDER description, empty features |
| Library write fails | Identification proceeds without citations (degraded) |
| Insect transaction fails | No partial writes (atomic) |

## Future: Event-Based Optimization

The current design is synchronous (CQS). The seam for future event-driven optimization:

- **Trigger point:** the moment a new rank entity is created (order, family, genus)
- **Event payload:** the rank name + authority reference
- **Async handler:** fetches authority content, generates description + features,
  updates the rank entity, creates citations
- **Benefit:** the identification transaction completes immediately with PLACEHOLDERs;
  enrichment happens asynchronously and the catalog page updates when ready

This refactoring changes only the orchestration shape, not the domain types or
repository contracts. The capability built synchronously today becomes the event
handler's implementation tomorrow.