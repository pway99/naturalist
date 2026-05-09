# Vision-Assisted Identification — Sketch (Deferred)

A console- and web-driven workflow that lets a naturalist point at an
image (insect photo, plant frame, soil structure shot), get a Claude
Vision identification, review the findings against the relevant domain
api shape, and persist the result through `EntityCommand`.

This is a **sketch**, not a binding plan, and it is currently
**deferred** — see the *Why deferred* section. Promote to numbered
milestones when manual console entry has surfaced enough friction to
justify the architecture.

---

## Why this exists (eventual use case)

Pat regularly identifies insects, plants, and (eventually) other
organisms from photos taken at Oak Vista. The current path is:

1. Take a photo.
2. Identify by hand (taxonomy lookup, common-name search, photo
   comparison against published references).
3. Curate descriptions across four Durrell levels.
4. Hand-edit JSON to add the entry.

Steps 2–4 take meaningful time per entry. Once console-driven entry is
durable (per `runtime-data-persistence.md`), the friction shifts from
"writes evaporate on restart" to "manual transcription is slow."
Vision-assisted identification compresses steps 2–4 into:

1. Take a photo.
2. Drop the image into the console (or future web app).
3. Review the model's identification against the domain api shape.
4. Confirm → `EntityCommand` inserts; runtime store persists.

The model is not authoritative — it produces a **draft** that Pat
reviews and confirms. Hallucinations, low-confidence matches, and
ambiguous taxonomies are edited or rejected before insert.

---

## Why deferred

1. **Pressure test doesn't justify it.** The *Battus philenor*
   pressure test stresses cross-domain shape, not catalog volume.
   Adding AI-assisted entry grows breadth but doesn't tighten the
   trilateral story; the pressure test argues for finishing Phase 1b
   evaluation first, not for accelerating data entry.
2. **Manual entry hasn't been tried yet.** Until
   `runtime-data-persistence.md` ships and Pat enters records through
   the console for a few weeks, the actual friction point is unknown.
   The right vision UX depends on what manual entry feels like; build
   the assist when the friction is named.
3. **Prompt + tool-schema design will iterate.** Confidence
   presentation, draft-review-confirm flow, and the right tool-call
   schema benefit from learning against real entries. Locking now
   risks cementing a wrong shape.
4. **Architectural shell is non-trivial.** Multi-PR effort: kernel
   facade, Anthropic SDK adapter, per-domain prompt + mapping,
   console route, API key management, image preprocessing. Worth doing
   well, not bolted on while five other efforts are in flight.

**Revisit when.** Console-driven manual entry has been Pat's daily
workflow for ≥4 weeks AND he can name the specific bottleneck
(probably: "describing four Durrell levels is the slow part" — which
is exactly what an LLM is good at).

---

## Architectural decisions (declarative, do not re-derive)

**Kernel facade, not direct SDK use.** Following the precedent of the
`Resilience` facade ([ADR-026](../adr/) and `kernels/framework`'s
`com.naturalist.resilience` package), introduce
`kernels/vision/` exposing a vendor-neutral `VisionService` interface.
Domain code calls the facade only; the Anthropic Java SDK lives behind
the adapter (`adapters/anthropic-vision/`). A future swap to a
different vision model (Gemini, GPT-4o, OpenCV-only local model,
domain-specific Vision Transformers) means a new adapter, not domain
changes.

**Tool use, not free-text parsing.** Claude returns structured data via
a tool call: per-domain tools like `propose_insect_species`,
`propose_plant`, `propose_phytochemical_constituent`. The kernel
accepts a `Tool<T>` definition; the adapter posts it; the result is a
typed `IdentificationDraft<T>`. No regex-parsing of model prose.

**Drafts are first-class.** Vision returns
`IdentificationDraft<InsectSpecies>` (or `<Plant>`, etc.) — a record
carrying:

- The candidate entity (record-shape, but invariants relaxed —
  description fields may be partial, common names may be empty).
- A confidence score (0.0–1.0).
- Up to N alternative candidates with their own confidence.
- Supporting evidence (which observed features drove the identification).

A draft is **not** an `EntityCommand.insert()` argument directly. The
console renders the draft for review; a separate explicit confirm step
maps it to a fully-formed entity and calls `command.insert(...)`.

**Image preprocessing happens in the kernel.** Resize, recompress, and
encode-to-base64 for the SDK is vendor-neutral. The kernel accepts a
`Path` or `byte[]`, normalises to a target dimension (default 1024px
longest side; configurable), and produces the SDK payload. Anthropic
Vision charges per image-tile (~1568×1568 grid); 1024-px resizing
keeps one image at a single tile cost.

**Per-domain prompts and tool schemas in `<domain>-vision/` modules.**
Insects-vision, plants-vision, etc. — each contributes its tool
definition and result-mapping. The kernel knows the shape of the
contract; the domain knows the prompt vocabulary and the entity
mapping. Mirrors the `CatalogContribution` pattern.

**API key from environment, not config files.** `ANTHROPIC_API_KEY`
env var, read at adapter construction. Refuse to start if absent and
the vision feature is wired. No defaults, no fallback — a missing key
is a clear configuration error.

**Resilience-wrapped.** The Anthropic API call is bulkheaded,
timed-out, retried per the kernel's `Resilience` facade. A flapping
upstream API degrades vision-assisted entry without touching the rest
of the console.

---

## Sketch — module layout

```
kernels/
  vision/                         (new kernel)
    VisionService.java            — facade
    Image.java                    — value object (path | bytes | metadata)
    IdentificationDraft<T>.java   — typed draft record
    Tool<T>.java                  — vendor-neutral tool definition
    NoOpVisionService.java        — default for tests / unwired apps

adapters/
  anthropic-vision/               (new adapter)
    AnthropicVisionService.java   — wraps the Anthropic Java SDK
    AnthropicVisionConfig.java    — model name, max-tokens, temperature
    ImagePreprocessor.java        — resize + base64 + encode
    pom.xml                       — depends on com.anthropic:anthropic-java

domains/
  insects/insects-vision/         (new module)
    InsectIdentificationTool.java — Tool<InsectSpecies> definition
    InsectIdentificationPrompt.java — system + user prompt strings
    InsectDraftMapper.java        — Tool result → InsectSpecies record
    pom.xml                       — depends on insects-api, vision

  plants/plants-vision/           (analogous; if/when needed)
  chemistry/chemistry-vision/     (probably not vision-applicable; defer)

apps/
  management-console/             (consumes everything)
    com/naturalist/console/vision/
      InsectVisionController.java — POST image, render draft, POST confirm
      PlantVisionController.java
      vision/insect-draft.jte     — review template
      vision/plant-draft.jte
```

---

## Workflow (end-to-end)

1. **Upload.** Pat drags an image into a `/insects/identify` form;
   `multipart/form-data` POST.
2. **Preprocess.** `ImagePreprocessor.normalise(image)` — resize to
   ≤1024 px longest side, recompress to JPEG quality 85, base64-encode.
3. **Call.** `visionService.identify(insectIdentificationTool, image)`
   posts the preprocessed image + system prompt + tool definition to
   Claude. Resilience-wrapped: retry on transient, timeout cap, circuit
   breaker on sustained failure.
4. **Draft.** Claude calls `propose_insect_species` with arguments;
   `InsectDraftMapper` converts them to
   `IdentificationDraft<InsectSpecies>` with confidence + alternatives.
5. **Review.** Console renders `vision/insect-draft.jte` with the
   draft and an editable form. Pat edits descriptions, adjusts
   taxonomy, picks among alternatives, fills in any required fields
   the model couldn't infer.
6. **Confirm.** Form POST hits `confirmDraft`; the controller
   constructs the final `InsectSpecies` record, calls
   `insectCommand.species().insert(...)`, which routes through the
   repository and the `JsonRuntimeStore` from
   `runtime-data-persistence.md`.
7. **Catalog re-assembly** *(deferred)*. Until the catalog kernel
   gains a re-assembly trigger, the new entity is searchable on next
   restart. With the trigger, it appears immediately. Coordinate with
   `catalog-kernel.md`'s open follow-up.

---

## Open design questions

- **Multi-image vs single-image.** Many insects need a dorsal + ventral
  + side photo for confident ID. Tool schema should accept up to N
  images per call. Token cost grows linearly; reserve for high-stakes
  entries.
- **Common-name population.** Vision is unreliable on local common
  names ("Pipevine Swallowtail" vs "Battus philenor"). Lean on the
  model for taxonomy + description; fill common names by hand or via a
  separate "do you know other names for this?" follow-up call.
- **Confidence threshold for auto-insert.** Probably never — every
  draft requires human confirmation. But a `confidence < 0.5` UX could
  prompt Pat to upload more photos before continuing.
- **Description-tier prompts.** Each Durrell level (preschool /
  elementary / secondary / university) is its own prose voice. Either
  a single tool-call returns all four (longer, single-shot), or a
  follow-up call elaborates the chosen draft (cheaper per call,
  multiple round trips). Lean single-shot for now; revisit.
- **Photo storage.** Where do uploaded images live? Today
  `domains/insects/insects-console` reads HEIC from
  `src/main/resources/insects/images/` and converts via `sips`. A
  console-uploaded photo needs a destination path; reuse the runtime
  data directory from `runtime-data-persistence.md`?
- **Cost cap.** A simple-minded user could fire 100 vision requests in
  an afternoon. Per-day token-budget enforcement at the kernel facade
  is cheap insurance.
- **Web app vs management console.** Pat mentions a future web app.
  The kernel facade is reusable across both; controllers per app.
- **Model and prompt caching.** Anthropic's prompt caching benefits
  this workflow heavily — the per-domain system prompt + tool
  definition is the same across every call; only the image varies.
  Use prompt caching from the start.

---

## Out of scope

- **Identification of organisms outside the implemented domains.** A
  photo of a fungus has no tool to call until a `fungi` domain ships.
  The system fails clearly ("no tool registered for this image
  category") rather than guessing across domains.
- **Field-side mobile capture.** A mobile-friendly capture UX is an
  app-level concern; the kernel facade is mobile-agnostic.
- **Training a custom model.** Claude Vision is the substrate; no
  fine-tuning of our own model.
- **Bulk-import workflows.** Drop a folder of 200 photos and let it
  rip — interesting but a different effort with different cost
  considerations. Defer.
- **Image storage strategy at scale.** S3 / CDN / etc. Out of scope
  for the in-process console era.
- **Identification of the trilateral *Battus philenor* story.** The
  pressure test's worked example is already fully described. This
  effort is for *new* records, not re-doing the worked example.

---

## How this slots into the work order

Per `docs/work-tracker.md`, this sits **after** the near-term queue
(command-framework → runtime-data-persistence → first console route →
FU-1 PR-2f/g/3 → catalog-kernel M9b–M12) and **after** ≥4 weeks of
manual console entry has revealed where the friction actually is. It
also wants the catalog re-assembly trigger landed (under
`catalog-kernel.md`) so newly-identified entities are searchable
without restart.

If the friction shows up earlier or differently than expected, this
plan adjusts; the sketch is the design space, not a commitment.
