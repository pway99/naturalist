# Vision-Assisted Identification MVP — Design

The management console gains two photo-driven workflows for building an
insect collection: *vision-assisted identification* (Claude identifies
an insect from a photo) and *direct observation* (the user knows what
it is). Both share client-side image handling, server-side storage,
and field-note maintenance on Linnaean rank pages.

The app's value over raw Claude Chat is the authoritative reference
layer — citations, EOL authority, catalog cross-links — that lets the
user verify identifications on their own timeline.

---

## Guiding decisions

1. **Claude's suggestion goes straight into the shared catalog.** No
   draft/pending/accept-reject lifecycle. The catalog is a reference
   library, not a commitment. The user's collection (their
   `FieldObservation` records) represents personal truth. Over time the
   catalog grows and the user has more species to choose from.

2. **Images are hosted server-side.** Cost is negligible for a personal
   app (~$0.12/month for 1,000 photos on S3-class storage). Hosting
   enables viewing the collection from any device. EXIF is stripped on
   upload; location and date arrive as structured metadata.

3. **Client-side resize before upload.** Browser JS resizes to 1024px
   longest side, JPEG quality 85 (~150–300 KB). This is both the
   Claude API input and the stored image. No full-resolution archival
   for MVP — 1024px is sufficient for a field reference. One upload,
   fast on cell connections.

4. **Field notes live on rank pages, not a journal.** Users browse from
   Linnaean rank starting at order. Each rank page shows the
   naturalist's images with editable field notes. When identification
   narrows, notes travel with the observation.

5. **Location enriches identification.** GPS from EXIF is
   reverse-geocoded to a place description ("Deer Creek, Butte County,
   CA") and sent alongside the image to Claude, narrowing range maps
   and eliminating look-alike species from other regions.

---

## Entry points

### Path 1 — "Identify this"

New route `GET /insects/identify` renders an upload form. The user
selects a photo (camera capture on mobile, file picker on desktop).
Browser JS:

1. Reads the file, extracts EXIF GPS coordinates and date taken.
2. Reverse-geocodes GPS to a place description (lightweight client-side
   lookup or editable text field pre-populated from coordinates).
3. Resizes to 1024px longest side, re-encodes as JPEG quality 85.
4. Sends multipart POST to `/insects/identify` with the resized image,
   location string, captured timestamp, and optional field notes.

Server receives the image and metadata, then:

1. Stores the image to the filesystem under a UUID filename.
2. Calls `VisionService.identify()` with the image, the insect tool
   schema, and a system prompt that includes the location context.
3. Deserializes the tool result into species data (taxonomy,
   descriptions, guilds, common names).
4. If the species is new to the catalog, calls
   `InsectCommand.species().insert(...)` to create the catalog entry
   with four-level Durrell descriptions.
5. Creates a `CitationAssociation` with an EOL reference URL for the
   suggested species, giving the user an authoritative starting point.
6. Creates an `InsectImage` at the suggested rank.
7. Creates a `FieldObservation` linked to the image, carrying the
   user's field notes, location, and vision confidence.
8. Redirects to the rank detail page for the suggested species.

The user lands on the species page and sees: the catalog entry Claude
created, the citations (EOL link to verify against), their photo with
field notes, and the existing catalog cross-links, clade navigation,
and authoritative references.

### Path 2 — "I know what this is"

On any rank detail page, the existing "Add photo" form is enhanced to
accept uploads from the user's device (replacing classpath-only image
references). Same EXIF extraction and resize. No Claude call.

The form adds:
- Image picker (`<input type="file" accept="image/*" capture="environment">`)
- Location (auto-populated from EXIF, editable)
- Date (auto-populated from EXIF, editable)
- Field notes text area

Submit creates `InsectImage` + `FieldObservation`, reloads the page
showing the new image with notes.

---

## FieldObservation model changes

Two new fields on the existing `FieldObservation` record:

```
location:    @Nullable String     ("Deer Creek, Butte County, CA")
confidence:  @Nullable Double     (0.0-1.0, set when vision-identified)
```

`location` is the reverse-geocoded place description from EXIF. Enriches
field notes and future vision calls.

`confidence` is Claude's identification confidence. Provenance metadata,
not a lifecycle field. Present when vision-identified; absent when the
user filed the observation manually.

Evidence and alternative candidates from Claude's response fold into the
`notes` field — either appended automatically ("Vision notes: ...") or
presented in the form for the user to incorporate. The notes field is the
natural home for narrative context about a sighting.

Existing fields (`id`, `observedBy`, `subject`, `observedOn`, `notes`)
are unchanged. The two-field addition ripples to all
`new FieldObservation(...)` call sites (test sources, controllers,
JSON fixtures).

---

## Field notes on rank pages

The existing image gallery on rank detail pages gains a field-notes
layer for the current naturalist's collection images.

For each image with a `FieldObservation`:
- The photo renders with its field notes (editable inline — text area,
  saves on submit).
- Location and date display from the observation metadata.
- If vision-identified, confidence is visible as context.

Images without a `FieldObservation` (shared catalog images) display as
they do today.

### Identification narrowing

When a user has an observation at genus *Vanessa* and later decides it
is species *Vanessa cardui*:

- From the genus page, the user sees their observation with images and
  notes.
- A "re-identify" action lets them select a more specific rank.
- `FieldObservation.subject` and `InsectImage.parentName` update to the
  species via `EntityCommand.update()`.
- Images and notes move to the species detail page.

### New console actions

```
POST /insects/{name}/notes        → update field notes on an observation
POST /insects/{name}/re-identify  → narrow observation to a more specific rank
```

---

## Vision kernel

**`kernels/vision/`** — a thin port following the Resilience facade
precedent.

```java
// Port — domain code calls this
public interface VisionService {
    ToolResult identify(Image image, ToolSchema tool, String systemPrompt);
}

// Value objects
public record Image(byte[] bytes, String mediaType, ImageMetadata metadata) 
    implements ValueObject { }

public record ImageMetadata(@Nullable String location, @Nullable Instant capturedAt) 
    implements ValueObject { }

public record ToolSchema(String name, String description, String parametersJson) 
    implements ValueObject { }

public record ToolResult(String toolName, String argumentsJson) 
    implements ValueObject { }
```

`NoOpVisionService` — default for tests and apps that don't wire
vision. Throws `UnsupportedOperationException` or returns an empty
result.

The kernel knows nothing about insects, species, or Anthropic. It
passes a tool schema and gets back a tool result as raw JSON. The
domain defines the schema and deserializes the result.

---

## Anthropic adapter

**`adapters/anthropic-vision/`** — wraps the Anthropic Java SDK.

```java
public class AnthropicVisionService implements VisionService {
    // Prompt caching: system prompt + tool schema are cache-stable
    // across calls; only the image varies.
    // Resilience-wrapped: @Resilient(name = "vision.identification")
    // API key from ANTHROPIC_API_KEY env var.
}
```

Configuration: model name (claude-sonnet-4-5 for good vision at lower
cost), max tokens, temperature.

No server-side image preprocessing — the client handles resize and
encoding.

---

## Insect identification service

A service class in `insects-core` (not a separate module) that owns:

- **Tool schema definition.** The JSON schema for Claude's
  `propose_insect_species` tool — fields for taxonomy, four-level
  Durrell descriptions, common names, guilds, beneficial flag,
  sighting notes, confidence, evidence, alternative candidates.

- **System prompt.** Instructs Claude to identify the insect, use the
  location context, describe at all four Durrell levels, assess
  confidence, and list alternatives. The identify-insect skill has
  working prompts that transfer directly.

- **Result deserialization.** Maps the `ToolResult.argumentsJson` into
  an `InsectSpecies` record plus confidence, evidence, and
  alternatives.

This class calls `VisionService.identify()` and returns a
domain-typed result that the console controller uses to create catalog
entries, observations, images, and citations.

---

## Image storage

**Filesystem, UUID filenames.** Images stored under a configurable
directory (e.g., `data/images/insects/`). Each image gets a UUID-based
filename (`019f0001-a001-7001-8001-a00000000001.jpg`).
`InsectImage.resourceName` stores this filename.

The console serves images from this directory via a static resource
mapping or a controller endpoint.

**EXIF handling:** Client-side re-encode through canvas strips EXIF.
Location and timestamp arrive as structured form fields, stored on
`FieldObservation`. No metadata embedded in the stored image file.

**Security:**
- Validate content type server-side (check magic bytes, not just
  extension). Reject anything that isn't JPEG/PNG/WebP.
- Re-encoding through the image library neutralizes embedded exploits.
- Serve with `Content-Disposition` or from a separate path to prevent
  stored XSS.
- Size limit: 20 MB per upload (pre-resize on client; post-resize
  images are ~150-300 KB).
- Authenticated access only — images are behind login.

**Privacy:** EXIF GPS is extracted client-side and sent as structured
data. The stored image contains no location metadata. Location is a
field the user controls and can omit.

No S3, no CDN for MVP. Filesystem with a mounted volume for hosted
deployment. Swap to object storage later without touching the domain.

---

## Mobile considerations

- `<input type="file" accept="image/*" capture="environment">` opens
  the camera on mobile devices.
- Single-column form layout for phone screens.
- Client-side resize keeps uploads fast on cell connections (~150-300
  KB vs 5-8 MB for a raw phone photo).
- The identification round-trip (upload + Claude API call) takes a few
  seconds; a loading indicator is needed.

---

## Module and dependency summary

### New modules (2)

| Module | Type | Contents |
|--------|------|----------|
| `kernels/vision/` | Kernel | `VisionService`, `Image`, `ImageMetadata`, `ToolSchema`, `ToolResult`, `NoOpVisionService` |
| `adapters/anthropic-vision/` | Adapter | `AnthropicVisionService`, `AnthropicVisionConfig` |

### Changed modules

| Module | Changes |
|--------|---------|
| `domains/insects/insects-api/` | `FieldObservation` gains `location` + `confidence` fields |
| `domains/insects/insects-core/` | `InsectIdentificationService` — tool schema, prompt, result mapping, orchestration |
| `domains/insects/insects-console/` | `/insects/identify` route, enhanced image upload on rank pages, field notes editing, re-identify action |
| `domains/insects/insects-repository-test/` | Updated `FieldObservation` test data and fixtures |
| `apps/management-console/` | Wires `VisionService` bean, image storage config, static resource mapping |

### DAG compliance

```
anthropic-vision    →  vision (kernel)
insects-core        →  insects-api, vision (kernel)
insects-console     →  insects-core, insects-api
management-console  →  anthropic-vision, vision, everything else (composition root)
```

Citation creation (catalog entry + `CitationAssociation`) is
orchestrated in the console controller, which already has access to
both the insects and library domains through the composition root. This
avoids adding a cross-domain dependency to `insects-core`.

---

## Out of scope

- **Full-resolution image archival.** 1024px resized images only for
  MVP.
- **Standalone field journal page.** Browse by rank, not by date.
- **Per-image captions.** Observations have one set of notes; multiple
  images of the same encounter share them.
- **Bulk identification.** One photo at a time.
- **Multi-image identification.** One image per Claude call. Multi-angle
  identification (dorsal + ventral) is a future enhancement.
- **Other domains.** Insects only. Plants-vision, etc. extracted to
  per-domain modules when needed.
- **Real EOL API client.** Citations link to EOL search URLs, not
  resolved page IDs. Phase 4 of the identification roadmap adds the
  real client.
- **Per-domain vision modules.** Tool schema and prompt live in
  `insects-core`. Extract to `insects-vision/` when a second domain
  needs vision.
