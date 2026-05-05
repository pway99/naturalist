# Phase 1b — Findings

**Status.** Active. Phase 1b in progress.

**Purpose.** The running record of the pressure test: per-finding strain
observed, language proposed, structural shape committed, JSON instance
fragment, closure status. Findings drive code change in real time when
confidence is high (charter §3).

**Sibling documents.**

- `00-charter.md` — governance, the five evaluation axes, severity tiers,
  finding template, discipline rules. Read first.
- `phase-1a-orientation.md` — the shared map of nodes and edges the
  swallowtail story exercises.
- `structural-commitments.md` — the six pre-Phase-1b identity-model
  commitments. Findings touching identity reference this document rather
  than re-deriving the convention.
- `99-followups.md` — rolling registry of deferred items.

**Resumption note.** A new chat reading `00-charter.md`,
`phase-1a-orientation.md`, `structural-commitments.md`, and this document
in that order has enough context to continue Phase 1b without backstory.
The session log at the end is the connective tissue across chats.

---

## 0. Conventions used in this document

### Finding ID

Stable, of the form `A{axis}-F{number}`.

- Axes 1–5 are the evaluation axes (charter §4).
- Axis 0 is reserved for cross-axis or charter-level findings.
- IDs are never renumbered once assigned.

### Severity

`BLOCK` / `STRAIN` / `SMELL` / `NOTE` (charter §5).

### Closure status

- `OPEN` — diagnosed; language and/or structural shape not yet proposed.
- `CONTINGENT (depends on …)` — diagnosed and language proposed, but
  closure depends on a node not yet evaluated, missing vocabulary, or a
  cross-cutting concern.
- `CLOSED` — language proposed, structural shape committed in Java by
  Pat, and bundle JSON expresses the new shape.

### Missing-vocabulary tokens

Names in chevrons (e.g. `«SecondaryMetabolite»`) mark proposed-but-unmodeled
vocabulary so it stays grep-able and unambiguous.

---

## 1. Findings

### A1-F1 — Living-organism slug mismatch (vernacular ↔ binomial)

**Severity.** STRAIN.

**Context.** Phase 1a orientation §7 surfaced this in production data.
The swallowtail's `larva.hostPlants : ["aristolochia-californica"]` does
not resolve to any `Plant.name` in the catalog (the plant is slugged
`california-pipevine`). The plants-side `PhytochemicalConstituent`
records use `california-pipevine`, consistent with the plant. The strict
typed-reference path between insects and plants is broken; the catalog
kernel's runtime token search would route around it via taxonomy and
common names, but the compile-time edge is silently broken on the
swallowtail's central reference.

**Strain observed.** Vernacular slugs (`california-pipevine`,
`pipevine-swallowtail`, `tomato`) are locally readable but globally
unstable. Cross-references between domains can carry "the right name for
the thing" while still failing to resolve, because the slug each side
chose is different. The bundle's `_meta` self-reports the mismatch as
known data; the catalog data carries the same mismatch as live state.
Beyond this single case, the convention itself does not generalize:
under multi-tenancy, regional vernacular collisions ("robin" naming
different birds) become unavoidable, and the convention has no rule for
resolving them.

**Language proposed.** Living-organism `EntityName` slugs encode the
**binomial name in lowercase kebab form**: `genus-species`. Vernacular
forms move to `Set<CommonName>` (already the load-bearing surface for
plants; added to insects). The slug is the **identity**; vernacular forms
are **findable but not authoritative**. See `structural-commitments.md`
§§2–3.

Two kernel interfaces codify the contract:

- `LinnaeanSpecies` — exposes genus and species epithets; asserts both
  non-null; provides the binomial slug as a default method derived from
  the epithets.
- `LinnaeanSubspecies` — exposes the parent species reference (typed via
  the framework's `Named<NAME>` generic pattern) and the trinomial
  epithets; provides the trinomial slug as a default method.

The slug is **derived**, not assigned (Commitment 4). An entity at
species rank cannot disagree with itself: `genus = Aristolochia` cannot
coexist with `slug = california-pipevine`, because the slug is a
function of the genus and species epithets.

**Structural shape.**

- DDD building block: kernel interfaces (`LinnaeanSpecies`,
  `LinnaeanSubspecies`); existing aggregates `Plant` and `InsectSpecies`
  implement `LinnaeanSpecies`. No new aggregates are introduced.
- Home module: `kernels/taxonomy` — same kernel that already houses
  `TaxonomicClassification`. Shape-shared across living-organism domains
  is exactly the kernel-candidacy criterion.
- Identity branch: slug (`NamedEntity`). Path A. Surrogate-identity
  promotion (Path B) considered and rejected (`FU-4`).
- Edges: every cross-domain reference field carrying old slugs is
  updated in lockstep — `LarvaStage.hostPlants`, `AdultStage.nectarSources`,
  `LarvaStage.parasitoidHosts`, `PhytochemicalConstituent.plantName`,
  composite-key inputs on `LifeStageName`,
  `PhytochemicalConstituentName`, `CultivarName`, `SeedLineageName`, and
  any others discovered during migration.
- Composite-key inheritance: composite slugs that compose from a
  living-organism slug rederive automatically because their inputs
  change. `LifeStageName` becomes `battus-philenor-larva` (was
  `pipevine-swallowtail-larva`); `PhytochemicalConstituentName` becomes
  `aristolochia-californica-aristolochic-acid-i` (was
  `california-pipevine-aristolochic-acid-i`).

**Migration scope.** Per `structural-commitments.md` §7:

| Catalog file                                            | Records affected | Type of change                           |
|---------------------------------------------------------|------------------|------------------------------------------|
| `plants/plants.json`                                    | 19               | Slug changes from vernacular to binomial |
| `insect-species.json`                                   | 16               | Slug changes from vernacular to binomial |
| `life-stages.json`                                      | 60               | Composite slug rederives                 |
| `plants/phytochemistry/phytochemical-constituents.json` | 4                | Composite slug rederives                 |
| `plants/cultivar/cultivars.json`                        | 4                | Composite slug rederives (verify)        |
| `plants/heritage/seed-lineages.json`                    | 6                | Composite slug rederives (verify)        |
| `plants/management/plant-programs.json`                 | 8                | May or may not rederive (verify pattern) |

Plus every cross-domain reference field carrying old slugs updates to
match. Migration is algorithmic — for each affected record, the new slug
is computed from current taxonomic data; for each cross-reference, the
slug is updated to match its target's new slug. There is no per-record
judgment.

Migration runs as a **single coordinated batch** — slugs do not migrate
piecemeal, because cross-references would break in transit.

**JSON instance fragment.** The swallowtail story under the new convention.
This is a sketch of the *targets*, not a reproduction of full records;
closure requires the full bundle and catalog data to express the new
shape.

```json
{
  "plant": {
    "name": "aristolochia-californica",
    "commonNames": [
      {
        "label": "California Pipevine",
        "locale": "en-US"
      },
      {
        "label": "California Dutchman's-Pipe",
        "locale": "en-US"
      }
    ],
    "taxonomy": {
      "order": "Piperales",
      "family": "Aristolochiaceae",
      "genus": "Aristolochia",
      "species": "californica"
    }
  },
  "insectSpecies": {
    "name": "battus-philenor",
    "commonNames": [
      {
        "label": "Pipevine Swallowtail",
        "locale": "en-US"
      },
      {
        "label": "Blue Swallowtail",
        "locale": "en-US"
      }
    ],
    "taxonomy": {
      "order": "Lepidoptera",
      "family": "Papilionidae",
      "genus": "Battus",
      "species": "philenor"
    },
    "larva": {
      "name": "battus-philenor-larva",
      "hostPlants": [
        "aristolochia-californica"
      ]
    }
  },
  "phytochemicalConstituent": {
    "name": "aristolochia-californica-aristolochic-acid-i",
    "plantName": "aristolochia-californica",
    "compoundName": "aristolochic-acid-i"
  }
}
```

**Closure status.** OPEN.

Closure requires three things, in order:

1. `kernels/taxonomy` grows `LinnaeanSpecies` and `LinnaeanSubspecies`
   interfaces, with the slug-derivation default methods.
2. `Plant` and `InsectSpecies` implement `LinnaeanSpecies`. `InsectSpecies`
   gains `Set<CommonName>`.
3. The migration runs in a single batch; bundle JSON and catalog JSON
   express the swallowtail story under the new slugs without referential
   breaks.

This finding is closed when (a) language is proposed (done above),
(b) Pat materializes 1–2 in Java, and (c) the bundle and catalog data
satisfy 3.

---

## 2. Session log

The connective tissue across chats. Five sentences is usually enough per
entry.

### Session N — Phase 1b kickoff (date TBD)

- **Node(s) worked on.** None yet — this entry opens the document.
- **Decided.** `A1-F1` logged with proposed language and structural shape
  per `structural-commitments.md`. Closure plan named: kernel interfaces
    + migration. Status remains OPEN until Pat materializes the kernel
      change and the migration runs.
- **In flight.** `A1-F1` (CONTINGENT on kernel change, then migration).
- **Java/bundle changes.** None yet — `A1-F1` is the first finding and
  its closure work is the kernel addition + mass migration described in
  `structural-commitments.md` §7. Done as a single coordinated batch
  before per-node Phase 1b proceeds.
- **Next session.** Pat decides whether the next session executes the
  `A1-F1` migration (closing the finding) or starts node-by-node Phase 1b
  on `Compound` or `PhytochemicalConstituent` per orientation §11. Per
  charter §6, Pat decides at start of next session.
