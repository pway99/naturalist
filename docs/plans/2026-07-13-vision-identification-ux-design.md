# Vision identification UX — separate machine ID from user notes

**Date:** 2026-07-13
**Status:** approved, implementing
**Scope:** insects domain (api, core, console) + management-console CSS

## Problem

Vision identification results were being crammed into the naturalist's
field-notes field. `InsectsController.buildVisionNotes` concatenated the
user's notes + a machine string (`"Vision identification (82% confidence): …"`)
+ the **raw alternatives JSON** into `FieldObservation.notes`, and the
species detail template rendered that blob into an *editable* "Save notes"
textarea. Machine identification data and the user's own notes were
conflated, and alternatives surfaced as raw JSON (e.g.
`…"confidence": 0.06}]`).

The real `lygaeus-kalmii` observation shows the damage: the user's note was
just `"Buckwheat"`; everything after was machine text welded on.

## Design

**Chosen shape:** read-only identification block (confidence + "why" +
alternatives) separated from an editable user-notes field.

### Domain model (`insects-api`)

New top-level `ValueObject`, consumed by both `FieldObservation` (api) and
`InsectIdentificationResult` (core) — two consumers ⇒ top-level per ADR-013/020:

```java
public record Identification(double confidence, String evidence, List<Candidate> alternatives) {
    public record Candidate(String scientificName, @Nullable String commonName, double confidence) {}
}
```

`Candidate.scientificName` is a plain `String`, not `InsectSpeciesName`: a
runner-up candidate may not exist in the catalog.

`FieldObservation`: replace flat `@Nullable Double confidence` with
`@Nullable Identification identification`. `notes` becomes **user notes only**.
`identification == null` ⇒ a manual (non-vision) observation.

### Core

- `InsectIdentificationResult` carries a typed `Identification` (drops the raw
  `alternativesJson` string).
- `InsectIdentificationService.parseResult` parses the `alternatives` field
  (a JSON-array string, or array node) into `List<Candidate>` at the parse
  boundary. No raw JSON survives past it.

### Console

- `identify()`: `notes = user notes only`, `identification = result.identification()`.
  **Delete `buildVisionNotes`.**
- Carry-forward paths (`updateNotes`, `re-identify`) thread the existing
  `identification` through unchanged.
- Manual paths (`observe`, `addImage`) already pass null confidence ⇒ null
  identification; no change.
- `detail.jte` figcaption: read-only identification zone (confidence line,
  `<details>` "Why this ID?" with evidence, "Also considered" candidate list —
  no raw JSON) + separate editable "Your notes" form bound to `obs.notes()` only.
- `naturalist.css`: styles for the new figcaption zones.

### Ripple (arity change)

- Construction sites: `identify`, `updateNotes`, `re-identify` in the
  controller; `newEntity`/`ghostEntity`/`modifiedEntity` in
  `FieldObservationEntityRepositoryTest` and `FieldObservationCommandImplTest`
  (`modifiedEntity` populates a non-null `Identification` to honor the
  change-every-mutable-field contract).
- `InsectIdentificationServiceTest`: `result.confidence()`/`result.evidence()`
  → `result.identification().…`.
- `field-observations.json`: 4 seed rows `confidence: null` → `identification: null`;
  repair the real `lygaeus-kalmii` row — `notes: "Buckwheat"` + a proper
  `identification` object with evidence and the two parsed candidates.

## Out of scope

- Re-running identification / editing the machine ID by hand.
- Persisting per-candidate catalog links.
