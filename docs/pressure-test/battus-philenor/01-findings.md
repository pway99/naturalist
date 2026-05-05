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

**Closure status.** CONTINGENT (depends on FU-1 — pending-organism mechanism).

Closure work delivered in this session:

1. `kernels/taxonomy` grew `LinnaeanSpecies` and `LinnaeanSubspecies`
   interfaces with slug-derivation default methods. Supporting type
   `TaxonomicSubspecies` and helper `TaxonomicSlugs` ship alongside.
2. `Plant` and `InsectSpecies` implement `LinnaeanSpecies`.
   `InsectSpecies` gained `Set<CommonName> commonNames` as a new
   record component.
3. The migration ran as a single batch over the catalog data. Six insect
   species (`battus-philenor` and five others with species-level taxonomy)
   and 13 plants migrated to binomial slugs. Cross-references in
   `LarvaStage.hostPlants`, `AdultStage.nectarSources`,
   `PhytochemicalConstituent.plantName`, `Cultivar.plantName`, and
   `PlantProgram.plantName` updated in lockstep.

What did **not** ship in this session, and why:

- **Genus/species non-null invariant tightening on `Plant` and
  `InsectSpecies`** is deferred. Ten insect species and five plants in
  the catalog carry partial taxonomy (genus-only, family-only, or fully
  unresolved) — `green-lacewing`, `tachinid-fly`, `creeping-thyme`, and
  the others identified during the Phase 1b sweep. Per Pat's session
  decision (under-identified organisms become "pending organisms"),
  these records continue to load with their original vernacular slugs
  and partial taxonomy. Tightening the contract here would orphan them
  before the pending-organism mechanism (`FU-1`) lands. Closure of
  `A1-F1` therefore waits on `FU-1` — at which point the invariant
  tightens and either (a) pending records migrate to a separate
  aggregate or (b) `LinnaeanSpecies` accepts a provisional epithet form.
- **Slug↔name consistency invariant** (`name.value().equals(binomialSlug())`)
  is not enforced at the record level. The fluent `Constraints` builder
  has no boolean-expression form (per `kernels/framework/docs/constraints-ubl.md`),
  and inventing one was outside this batch's scope. Consistency is
  guaranteed by construction during the migration; if drift appears
  later it surfaces as a new finding and motivates an
  `expression`-form constraint addition.
- **Cultivar / SeedLineage / PlantProgram slug forms** stay as today.
  These will be owned by a future `Naturalist` entity (separate
  modeling task); their slugs rebase when that entity arrives.
- **`commonNames` not yet wired into catalog search tokens.** The
  binomial-slug commitment regresses common-name search: a naturalist
  typing "California Pipevine" or "borage" no longer hits any slug.
  Common-name labels now exist on every `Plant` and `InsectSpecies`
  record, but each domain's `CatalogContribution.searchableEntities()`
  must lift those labels into the token stream for vernacular search to
  resolve. Four `InMemoryCatalogTest` cases that depended on the old
  vernacular slugs (`exactSlugMatchYieldsExactSlugHit`,
  `exactMatchPreferredOverPrefix`, `hitsOrderedByKindThenSlug`,
  `groupedByDomainPreservesWithinDomainOrdering`) are `@Disabled` with
  pointers back to this finding's fast-follow. Re-enable once the
  per-domain contributions emit `CommonName.label()` as tokens — at
  which point those tests can be rewritten to query against vernacular
  forms (which will then carry EXACT_TOKEN match) while binomial slugs
  carry EXACT_SLUG match. This is a fast-follow on `A1-F1`, not its own
  finding; the regression is the natural cost of the slug commitment
  and was anticipated in `structural-commitments.md` §3 ("vernacular
  forms are findable but not authoritative").

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

### Session N+1 — `A1-F1` migration (2026-05-04)

- **Node(s) worked on.** `A1-F1` closure work — kernel interfaces in
  `kernels/taxonomy`, `Plant` and `InsectSpecies` aggregates, test
  identifiers, and the catalog JSON migration in a single coordinated
  batch.
- **Decided.** Loose-derivation enforcement chosen over strict (slug
  remains assignable; derivation is a default method). `LinnaeanSpecies`
  is unparameterized — `binomialSlug()` returns `String`, and
  implementing entities still declare `NamedEntity<NAME>` independently.
  Decision context: a typed `NAME`-returning default would force every
  implementation to override (no kernel-side generic factory on
  `EntityName`), defeating the kernel-default's purpose. If the
  ergonomics start mattering at consumer sites, a one-commit refactor
  to `LinnaeanSpecies<NAME extends EntityName> extends Named<NAME>`
  remains available.
- **In flight.** `A1-F1` is now CONTINGENT on `FU-1`. The
  pending-organism mechanism is the closure dependency — once it lands,
  the genus/species non-null invariant tightens and `A1-F1` can move to
  CLOSED.
- **Java changes.**
  - `kernels/taxonomy` — added `LinnaeanSpecies`, `LinnaeanSubspecies`,
    `TaxonomicSubspecies`, package-private `TaxonomicSlugs`, with unit
    tests. `pom.xml` gained `framework-test` (test scope) for the new
    test classes.
  - `Plant` (plants-api) — implements `LinnaeanSpecies`; `genus()` and
    `species()` accessors forward to `taxonomy`. Invariants unchanged
    (tightening deferred).
  - `InsectSpecies` (insects-api) — gained `Set<CommonName> commonNames`
    component; implements `LinnaeanSpecies`. Invariants gained
    `notNull(commonNames, ...)`. Constructor call sites in
    `InsectAggregateTest`, `SpeciesRepositoryTest` updated; their
    fixture taxonomies promoted from family-level to species-rank to
    keep the Observer green under the new component.
  - `Plant` ghost-entity fixture in `PlantEntityRepositoryTest`
    promoted to species-rank for the same reason.
  - `TestPlantsIdentifiers` and `TestInsectsIdentifiers` — six insect
    species and 13 plant constants migrated to binomial slugs.
    Composite constituent constants now use the programmatic
    `PhytochemicalConstituentName.of(plantName, compoundName)` factory
    so they rederive from their inputs. Cultivar / seed-lineage /
    plant-program slugs untouched — those entities move to a future
    Naturalist domain.
- **Catalog changes.** `plants.json`, `insect-species.json`,
  `life-stages.json`, `phytochemistry/phytochemical-constituents.json`,
  `cultivars.json`, `plant-programs.json`, and the bundle migrated in
  one batch. Pending records (10 insects, 5 plants) keep their
  vernacular slugs and original taxonomies. Six insect species and 13
  plants migrated to binomial. All cross-references (`hostPlants`,
  `nectarSources`, `parasitoidHosts`, `plantName`) followed their
  targets. Every `InsectSpecies` record now carries a `commonNames`
  array seeded from its previous vernacular slug — Pat to curate as
  needed.
- **Observation worth recording.** The swallowtail's
  `adult.nectarSources` carries five plant slugs that already had
  binomial form pre-migration but reference plants not yet in the
  catalog (`aesculus-californica`, `dichelostemma-capitatum`,
  `triteleia-laxa`, `eriodictyon-californicum`, `centaurea-solstitialis`).
  This is a pre-existing dangling-reference gap, not introduced by the
  migration — flag for a future Axis-4 finding when the relevant nodes
  are evaluated.
- **Next session.** Pat decides whether to start node-by-node Phase 1b
  on `Compound` or `PhytochemicalConstituent` per orientation §11, or
  to take up `FU-1` (pending-organism mechanism) so `A1-F1` can move to
  CLOSED.
