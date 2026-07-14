# Vision-Assisted Identification — Chat Briefing

**Purpose.** Describes the vision identification pipeline: how a photo
becomes a catalog entry. Covers the vision kernel, the Anthropic adapter,
the insect identification service, client-side image processing,
server-side image storage, and the console routes that tie them together.

Pair with `docs/briefings/insects-domain.md` for the domain model
(species, observations, images) and `docs/briefings/framework-core.md`
for structural vocabulary.

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-07-13), not
extrapolated. If you need a type not listed here, ask before inventing one.

---

## 1. Architecture Overview

```
Browser                  Console Controller           Domain / Kernel
──────                   ──────────────────           ──────────────
[Photo]                                                
  │ JS resize (1024px)                                 
  │ EXIF stub                                          
  ▼                                                    
POST /insects/identify ──► ImageStorageService.store() ──► data/images/insects/
                         │                               
                         │  Image(bytes, mediaType,      
                         │        ImageMetadata)          
                         ▼                               
                    InsectIdentificationService          
                         │  .identify(Image)             
                         │                               
                         ▼                               
                    VisionService.identify()             
                         │  (kernel port)                
                         ▼                               
                    AnthropicVisionService                
                         │  (adapter, tool_use)           
                         ▼                               
                    InsectIdentificationResult            
                         │  .species() → InsectSpecies   
                         │  .confidence(), .evidence()   
                         ▼                               
                    Insert species + image +             
                    FieldObservation into catalog         
                         │                               
                         ▼                               
                    redirect → detail page               
```

---

## 2. Vision Kernel — `kernels/vision`

Package: `com.naturalist.vision`. Vendor-neutral port following the
Resilience facade precedent: thin kernel port, adapter in `adapters/`.

### `VisionService` (port interface)

```java
public interface VisionService {
    ToolResult identify(Image image, ToolSchema tool, String systemPrompt);
}
```

Domain code calls this interface. The adapter implements it.

### `Image` — ValueObject

```java
public record Image(
    byte[] bytes,
    String mediaType,
    ImageMetadata metadata
) implements ValueObject
```

Raw image bytes, MIME type, and optional metadata (location, capture time).

### `ImageMetadata` — ValueObject

```java
public record ImageMetadata(
    @Nullable String location,
    @Nullable Instant capturedAt
) implements ValueObject
```

Both fields nullable — EXIF parsing is stubbed for MVP.

### `ToolSchema` — ValueObject

```java
public record ToolSchema(
    String name,
    String description,
    String parametersJson
) implements ValueObject
```

Describes the tool the model should use. `parametersJson` is a JSON Schema
string defining the tool's input shape.

### `ToolResult` — ValueObject

```java
public record ToolResult(
    String toolName,
    String argumentsJson
) implements ValueObject
```

The model's structured response — a JSON string of tool arguments to be
parsed by the domain service.

### `NoOpVisionService`

Default fallback. Throws `UnsupportedOperationException` on use — vision
is explicit opt-in, not silent degradation.

---

## 3. Anthropic Adapter — `adapters/anthropic-vision`

Package: `com.naturalist.vision.anthropic`. Implements `VisionService`
using the Anthropic Java SDK (`com.anthropic:anthropic-java`).

### `AnthropicVisionService`

- Reads `ANTHROPIC_API_KEY` from environment at construction; refuses to
  construct if absent.
- Sends the image as base64 in an `ImageBlockParam`.
- Forces tool use via `ToolChoice.ofTool(name)` so the response always
  contains structured data.
- Marks the system prompt with `cache_control: ephemeral` for prompt
  caching — the fixed prompt is cached across calls; only the image bytes
  vary.
- Carries `@Resilient(name = "vision.identification")`.

### `AnthropicVisionConfig`

```java
public record AnthropicVisionConfig(String model, int maxTokens) {
    public static AnthropicVisionConfig defaults() {
        return new AnthropicVisionConfig("claude-sonnet-4-5-20250514", 4096);
    }
}
```

Model is pinned to a date-stamped identifier to prevent silent upgrades.

### Wiring — `VisionConfiguration` (in `apps/management-console`)

```java
@Bean
VisionService visionService() {
    var apiKey = System.getenv("ANTHROPIC_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
        return new NoOpVisionService();
    }
    return new AnthropicVisionService(AnthropicVisionConfig.defaults());
}
```

No API key → `NoOpVisionService`. Set `ANTHROPIC_API_KEY` to activate.

---

## 4. InsectIdentificationService — `insects-core`

Package: `com.naturalist.insects`. Annotated `@DomainService`
(auto-discovered by `DomainServiceScan`). Orchestrates:

1. Builds a `ToolSchema` named `propose_insect_species` with a JSON Schema
   requiring: name, order, family, genus, species, commonName, four
   Durrell descriptions, guilds, beneficial, confidence, evidence.
2. Builds a system prompt (expert entomologist persona with Durrell
   instructions). Appends location context if provided.
3. Calls `visionService.identify(image, toolSchema, systemPrompt)`.
4. Parses the `ToolResult.argumentsJson()` into an
   `InsectIdentificationResult`.

### `InsectIdentificationResult`

```java
public record InsectIdentificationResult(
    InsectSpecies species,
    double confidence,
    String evidence,
    @Nullable String alternativesJson
)
```

The `species` is a fully formed `InsectSpecies` record ready for catalog
insert. Nullable value-object fields (chemicalDefense, voltinism, etc.)
are null — populated incrementally by the naturalist later.

### Tool schema fields

| Field                    | Type      | Required | Notes                                   |
|--------------------------|-----------|----------|-----------------------------------------|
| `name`                   | string    | yes      | Kebab-case slug (e.g. `battus-philenor`)|
| `order`                  | string    | yes      | Taxonomic order (e.g. `Lepidoptera`)    |
| `family`                 | string    | yes      | Taxonomic family                        |
| `genus`                  | string    | yes      | Taxonomic genus                         |
| `species`                | string    | yes      | Species epithet                         |
| `commonName`             | string    | yes      | Most widely used common name            |
| `descriptionPreschool`   | string    | yes      | Durrell preschool level                 |
| `descriptionElementary`  | string    | yes      | Durrell elementary level                |
| `descriptionSecondary`   | string    | yes      | Durrell secondary level                 |
| `descriptionUniversity`  | string    | yes      | Durrell university level                |
| `guilds`                 | string[]  | yes      | From FunctionalGuild enum               |
| `beneficial`             | boolean   | yes      | Garden/agriculture context              |
| `sightingNotes`          | string?   | no       | Notable observations                    |
| `confidence`             | number    | yes      | 0.0–1.0                                 |
| `evidence`               | string    | yes      | Visible features supporting ID          |
| `alternatives`           | string?   | no       | JSON array of alternatives              |

---

## 5. Client-Side Image Processing — `image-upload.js`

Located at `apps/management-console/src/main/resources/static/js/image-upload.js`.
IIFE that attaches to any form with `id="identify-form"`.

### Resize

- Resizes to 1024px on the longest side via canvas.
- Re-encodes as JPEG quality 0.85.
- Submits the resized blob, not the original file.

### EXIF

- `readExif(file)` scans for APP1 marker (0xFFE1).
- `parseExifApp1()` is a **stub** — returns `{}`. Full IFD parsing is
  deferred until manual-entry friction is named.
- When EXIF data is available, populates the location and capturedAt
  fields automatically.

### Form submission

- Prevents default submit.
- Builds `FormData` with the resized image blob and all text/hidden fields.
- Shows "Identifying..." on the submit button.
- Follows the redirect on success; shows alert on failure.

### Usage

Both the `/insects/identify` page and the species detail page "Add Photo"
form use `id="identify-form"` and include
`<script src="/js/image-upload.js"></script>`.

---

## 6. Server-Side Image Storage — `ImageStorageService`

Package: `com.naturalist.insects.console`. Package-private class in the
console module — not a domain service.

### `store(byte[] imageBytes) → FileName`

1. Validates size (max 20 MB).
2. Detects format from magic bytes:
   - JPEG: `FF D8 FF`
   - PNG: `89 50 4E 47`
   - WebP: `RIFF....WEBP`
3. Rejects anything else.
4. Generates a UUID filename via `EntityId.newUUID()` + detected extension.
5. Writes to `data/images/insects/` (configurable).
6. Returns `FileName.of(uuidName)`.

### `resolve(String filename) → Path`

Resolves a filename to a filesystem path. Validates against path traversal
(`normalize()` + `startsWith()` check).

### Image serving

`GET /insects/uploads/{filename}` serves uploaded images from the
filesystem. Content-Disposition uses the resolved path's filename (not the
raw parameter) to prevent header injection.

Pre-upload catalog images are served from classpath at
`GET /insects/images/{filename}`. The `detail.jte` template uses an
`onerror` fallback: tries `/insects/uploads/` first, falls back to
`/insects/images/`.

---

## 7. Console Routes

### `GET /insects/identify` — identify form

Renders `insects/identify.jte` with CSRF token. Accepts optional
`?identified=<slug>` query param to show a success message when a
novel-genus species was identified (can't redirect to detail page without
the genus in the catalog).

### `POST /insects/identify` — run identification

1. Stores the uploaded image via `ImageStorageService`.
2. Builds a vision `Image` with the bytes, metadata (location, capturedAt).
3. Calls `identificationService.identify(image)`.
4. Inserts the species into the catalog if new
   (`insectCommand.species().insert()`).
5. Creates an `InsectImage` linked to the species.
6. Creates a `FieldObservation` with confidence, location, and notes
   (combining user notes with vision evidence).
7. Redirects to the species detail page — unless the genus doesn't exist
   in the catalog (novel genus), in which case redirects back to
   `/insects/identify?identified=<slug>` to avoid an NPE in the
   rank-chain resolution.

### `POST /insects/{name}/images` — add photo to existing species

Multipart upload. Stores the image, creates a `FieldObservation` (if
signed in), creates an `InsectImage` with `observationId` link.

### `POST /insects/{name}/notes` — update field notes

Updates the notes on an existing `FieldObservation`.

### `POST /insects/{name}/re-identify` — re-identify observation

Updates the `subject` on a `FieldObservation` to a new rank name.
Image parentName updates are deferred (no image-by-observationId query
exists yet).

---

## 8. FieldObservation — Updated Shape

The vision MVP added two nullable fields to `FieldObservation`:

```java
public record FieldObservation(
    FieldObservationId id,
    NaturalistName observedBy,
    InsectRankName subject,
    Instant observedOn,
    @Nullable String notes,
    @Nullable String location,
    @Nullable Double confidence
) implements Entity<FieldObservationId>
```

`location` is a free-text string (e.g. "Deer Creek, Butte County, CA").
`confidence` is the vision model's identification confidence (0.0–1.0),
null for manual sightings.

---

## 9. Known Limitations (MVP)

- **No nav link** to `/insects/identify` — reachable only by URL.
- **EXIF parser is stubbed** — location and date must be entered manually.
- **Novel-genus redirect** — when vision identifies a species whose genus
  isn't in the catalog, the user sees a success message on the identify
  page instead of the detail page. No genus/family/order commands exist
  to create parent ranks automatically.
- **Citation creation skipped** — `CitationRepository` and
  `CitationAssociationRepository` are package-private; no public write
  API. Vision results include enough data for future citation creation.
- **Re-identify image updates deferred** — no `ImageQuery` method to find
  images by `observationId`.
- **Guilds/beneficial not consumed** — the tool schema returns guild and
  beneficial data, but the `InsectFunctionalRole` creation is not wired
  into the identification flow.

---

## 10. Anti-patterns

- **Do not invent a `VisionResult` domain entity.** The vision result is
  transient — it becomes an `InsectSpecies` + `FieldObservation` +
  `InsectImage`. No separate persistence.
- **Do not put vision types in a domain module.** The vision kernel is
  vendor-neutral infrastructure, not domain logic. Domain code consumes
  it through `InsectIdentificationService`.
- **Do not call `UUID.randomUUID()` in `ImageStorageService`.** Use
  `EntityId.newUUID()` for UUIDv7.
- **Do not serve uploaded images from classpath.** Uploaded images go to
  the filesystem (`data/images/insects/`); classpath images are
  pre-existing catalog photos.
- **Do not skip magic-byte validation.** The `ImageStorageService`
  validates content type from actual bytes, not the upload's
  Content-Type header.
