# Plants Domain

Coding conventions and structural rules for the plants domain. Narrative,
ecological, and historical context lives in
[`docs/briefings/plants-domain.md`](../../docs/briefings/plants-domain.md) — load that file when the
conversation is about the *what* and *why* rather than the *how*. Domain vocabulary and
the cross-domain relationship map live in [`docs/plants-ubl.md`](docs/plants-ubl.md).

## Plants is being brought into line with insects

Plants was one of the first domains built. Insects then became the focus, and it was in
modelling insects that the working answers emerged — how to carry Linnaean rank, how to
place an organism in a clade, and how to record a find at the rank its evidence supports.
Plants predates all of it.

**Three rules hold for every step of that alignment.** They are not milestones; they
govern whatever milestone is in flight. Current work is tracked in
[the consistency plan](../../docs/plans/2026-08-15-plants-domain-consistency-plan.md).

**1. Insects is the reference; plants moves — and insects holds still.** Where the two
disagree on shape — rank entities, name types, cross-rank attachment, read models — insects
is right by default and plants changes. Deviating needs a stated plants-specific reason
recorded here, not an inherited accident. The design itself is written up in
[`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md).

This is a **plants-only exercise**. Ideas that would improve insects surface constantly
while doing it — `InsectImage.parentName` and `FieldObservation.subject` would both read
better as `insectName`, for instance — and they are noted, not acted on. Changing the
reference mid-alignment means measuring against a moving target, and the discoveries worth
applying to insects are not all in yet. Circle back when plants is done.

**2. The catalog is test data, not fact — and it follows the model.** `plant-species.json`
and its siblings were authored before the rank layer existed. A binomial in a slug is a
*claim about identification confidence*, not a given — several rows named a rank the
evidence did not support. Assign each record to the most specific rank its evidence
actually supports and no further; this is the blueprint's B1 discipline applied
retrospectively, and it is the same rule whether the identifier is a vision model or a
human reading the catalog. Never reshape a record before its rank has been re-assessed — a
foreign key added to a wrong rank is far more expensive to undo than to get right first.

**Fixtures are reshaped to fit the model; a model is never bent to fit a fixture.** When a
new constraint rejects existing data, the finding is about the data. Expect JSON to fail on
first contact with a structure it predates, and fix the JSON. The one exception is the
standing rule that real Oak Vista measurements are evidence: if a *value* is right and the
model rejects it, that is a domain-model error worth stopping for. A missing parent record
is not that; it is a gap to author.

**3. Align first, abstract second.** Generalising into `kernels/taxonomy` or
`kernels/clades` waits until plants matches insects and the two can be read side by side.
An abstraction drawn from one finished domain and one mid-migration is drawn from noise.

**4. Model accuracy outranks a green build — during this migration only.** Getting the
types right is what makes the change reviewable; a compile error in a consumer is a
mechanical consequence, not a design signal, and blocking on it forces the model to arrive
in fragments shaped by what happened to still compile. So a milestone may land red, and
consumer fallout is repaired in a following step.

Three conditions on that, or it stops being a strategy and becomes a mess:

- **The red is named.** A commit that knowingly breaks the build says so in its message,
  and says which consumers are left broken. An unexplained red build is still a bug.
- **The debt is bounded.** Red is a state between two steps of one milestone, not a
  standing condition. Stabilise before starting an unrelated milestone.
- **The suspension is local.** This applies to the plants alignment. It is not a
  repo-wide licence, and it lapses when the migration lands.

## Sub-context layout

```
plants-api/com/naturalist/plants/
  PlantSpecies, PlantFamily, PlantGenus                        — botanical rank records
  PlantLifeForm, PlantRole                              — vocabularies
  PlantRepository, PlantQuery, PlantEntityCollections   — namespaces
  PlantsDomain                                          — DomainId subtype
  cultivar/        Cultivar, VarietyType, FruitType, SeedSavingPolicy
  heritage/        SeedLineage, Provenance
  management/      PlantProgram, PlantProgramRepository
  phytochemistry/  PhytochemicalConstituent, PhytochemicalCategory,
                   PlantTissue, InductionMode
                   role/  PhytochemicalRole (sealed interface, stateless permits)
```

Each directory is a sub-context. `package-private` is internal to the
sub-context; `public` crosses the boundary.

## Identity model

| Type                       | Location          | Identity                                    | Identifier class                                                     |
|----------------------------|-------------------|---------------------------------------------|----------------------------------------------------------------------|
| `PlantFamily`              | `plants/`         | `NamedEntity<PlantFamilyName>`              | `identifiers/.../plants/PlantFamilyName`                             |
| `PlantGenus`               | `plants/`         | `NamedEntity<PlantGenusName>`               | `identifiers/.../plants/PlantGenusName`                              |
| `PlantSpecies`                    | `plants/`         | `NamedEntity<PlantSpeciesName>`                    | `identifiers/.../plants/PlantSpeciesName`                                   |
| `Cultivar`                 | `cultivar/`       | `NamedEntity<CultivarName>`                 | `identifiers/.../plants/cultivar/CultivarName`                       |
| `SeedLineage`              | `heritage/`       | `NamedEntity<SeedLineageName>`              | `identifiers/.../plants/heritage/SeedLineageName`                    |
| `Provenance`               | `heritage/`       | `ValueObject`                               | —                                                                    |
| `PlantProgram`             | `management/`     | `NamedEntity<PlantProgramName>`             | `identifiers/.../plants/management/PlantProgramName`                 |
| `PhytochemicalConstituent` | `phytochemistry/` | `NamedEntity<PhytochemicalConstituentName>` | `identifiers/.../plants/phytochemistry/PhytochemicalConstituentName` |

No `Aggregate` or `Entity<UUIDv7>` records in this domain yet.

## Soft FK chain

All cross-entity references are by `EntityName` slug — no compile-time coupling
between sub-contexts beyond shared identifier classes.

```
PlantGenus.familyName               → PlantFamily.name  (PlantFamilyName)
PlantProgram.plantName              → PlantSpecies.name        (PlantSpeciesName)
Cultivar.plantName                  → PlantSpecies.name        (PlantSpeciesName)
SeedLineage.cultivarName            → Cultivar.name     (CultivarName)
PhytochemicalConstituent.plantName  → PlantSpecies.name        (PlantSpeciesName)
PhytochemicalConstituent.compoundName → chemistry Compound.name (CompoundName)
PlantSpecies.nativeBioregions              → kernels/biogeography Bioregion
```

`PlantGenus.familyName` is the only typed upward rank FK that exists today, and
`PlantGenusTestEntitySource` enforces it with a `ForeignKeyConstraint`. **`PlantSpecies`
carries no `genusName`** — its position in the hierarchy is held only as the
string-valued `taxonomy` component, so the rank chain terminates at genus and
`PlantSpeciesTestEntitySource` declares no foreign keys. Closing that gap, and resolving
the five organisms currently recorded as both a species-less `PlantSpecies` and a
`PlantGenus`, is tracked in
[`docs/plans/2026-08-15-plants-domain-consistency-plan.md`](../../docs/plans/2026-08-15-plants-domain-consistency-plan.md).

The `PhytochemicalConstituent.compoundName` reference is the only soft FK
in this domain that crosses *domain* boundaries (rather than just sub-context
boundaries within plants). The reference is via `CompoundName` from the
`identifiers` module — `plants-api` does **not** depend on `chemistry-api`.
Cross-domain referential integrity (the referenced compound exists in the
chemistry catalog) is a service-layer rule, not a record invariant.

## Repository and query namespaces

Every sub-context exposes a repository namespace class and a query namespace
interface, following the convention in `domains/CLAUDE.md`. Seven repositories
across five sub-contexts:

- `PlantRepository` (top-level package-private class) → `PlantEntityRepository`,
  `PlantFamilyEntityRepository`, `PlantGenusEntityRepository`
- `CultivarRepository` (cultivar) → `CultivarEntityRepository`
- `SeedLineageRepository` (heritage) → `SeedLineageEntityRepository`
- `PlantProgramRepository` (management) → `PlantProgramEntityRepository`
- `PhytochemicalConstituentRepository` (phytochemistry) →
  `PhytochemicalConstituentEntityRepository`

Read side, all public in api, adapters in `plants-core`:

- `PlantQuery` → `plants()`, `families()`, `genera()`
- `CultivarQuery` → `cultivars()` (+ `forPlantName`)
- `SeedLineageQuery` → `lineages()` (+ `forCultivarName`)
- `PlantProgramQuery` → `programs()` (+ `forPlantName`)
- `PhytochemicalConstituentQuery` → `constituents()`
  (+ `forPlantName`, `forCompoundName` — the cross-domain reverse lookup)

Collections: `PlantEntityCollections` (`PlantSpeciesCollection`,
`PlantFamilyCollection`, `PlantGenusCollection`) plus one `*EntityCollections`
namespace per sub-context. No aggregate factories exist yet — add them
package-private and concrete in `plants-core` when a read model materializes.

**Two known deviations from `domains/CLAUDE.md`**, both tracked in the
consistency plan rather than fixed piecemeal: the nested types carry the domain
prefix and an `Entity` infix (`PlantFamilyEntityQuery`) where the convention
drops both (`FamilyQuery`), and the four single-entity sub-contexts wrap their
lone type in a namespace where the N=1 collapse rule says to skip it.

## Domain-specific invariants

Every record below has an invariant test in `plants-api/src/test/java` covering
the valid shape, the all-null shape, and any domain-meaningful edge (empty role
set, blank provenance, nullable-by-design fields).

- `PlantFamily` — `name`, `order`, `family`, `description`, `commonNames`
  required. An empty `commonNames` set means *no asserted vernacular name yet*.
- `PlantGenus` — `name`, `familyName`, `order`, `family`, `genus`,
  `description`, `commonNames` required. The redundant `order`/`family`
  epithets are carried locally so a catalog-assembly chain check does not have
  to resolve the parent record.
- `PlantSpecies` — `name`, `taxonomy`, `description`, `roles`, `lifeForm`,
  `nativeBioregions`, `commonNames` are all required (non-null). An empty
  `nativeBioregions` set means *no asserted native range*, not *unknown*.
  `taxonomy.genus` and `taxonomy.species` are individually nullable — the
  taxonomy kernel permits family-level identification — so a `PlantSpecies` can
  currently be catalogued with no resolved species.
- `Cultivar` — `name`, `plantName`, `description`, `varietyType`, `fruitType`,
  `seedSavingPolicy` required. `commonName` non-blank. `seedSource` and
  `gardenNotes` nullable.
- `SeedLineage` — `name`, `cultivarName`, `provenance`, `description`
  required. **Service-layer rule:** the referenced cultivar must be
  `VarietyType.OPEN_POLLINATED`. Not enforced in record invariants (cross-entity).
- `PlantProgram` — `name`, `plantName`, `description` required. `constraint`
  and `notes` nullable; `hasConstraint()` predicate distinguishes the two
  program shapes (constraint-bearing vs pure schedule).
- `PhytochemicalConstituent` — `name`, `plantName`, `compoundName`,
  `description`, `category`, `induction` required (non-null). `roles` and
  `tissues` are non-empty sets — a constituent with no role is data without
  a story; an unknown tissue should be recorded as `WHOLE_PLANT` rather than
  an empty set. `notes` nullable. **Service-layer rule:** the referenced
  `compoundName` must exist in the chemistry catalog (cross-aggregate, not
  enforced in record invariants).
- `Provenance` (ValueObject) — `originator`, `originLocation` non-blank.

## Behavioral predicates on the PlantSpecies record

These are first-class API methods, not inline `roles.contains(...)` checks at
call sites:

- `PlantSpecies.isKeystoneHost()` — `roles.contains(PlantRole.KEYSTONE_HOST)`. Drives
  zero-pesticide constraints in the PestManagement application module.
- `PlantSpecies.supportsBiocontrolInsects()` — `roles.contains(BENEFICIAL_INSECT_HABITAT)`.
- `PlantSpecies.isNitrogenFixer()` — `roles.contains(NITROGEN_FIXER)`.
- `PlantSpecies.isNativeTo(Bioregion)` — membership check on `nativeBioregions`.

Cultivar has parallel predicates: `breedsTrueFromSeed()`, `requiresSeedSaving()`,
`isSauceVariety()`. SeedLineage has `hasActiveAdaptationProgram()`.

`PhytochemicalConstituent` provides axis-level rollups over its
`Set<PhytochemicalRole>` so consumers don't pattern-match the sealed
hierarchy at the call site:

- `playsRole(PhytochemicalRole)` — generic membership check.
- `isDefensive()` — any of `HerbivoreDeterrent`, `InsectDeterrent`,
  `AntiFungal`, `AntiMicrobial`, `Allelopathic`.
- `isSignaling()` — any of `InducedVolatileSignal`, `PollinatorAttractant`,
  `SeedDisperserAttractant`, `MycorrhizalSignal`.
- `mediatesEnvironmentalStress()` — any of `UVProtectant`, `StressTolerance`,
  `HeavyMetalChelator`.
- `hasMedicinalApplication()` — `Pharmaceutical` or `Nutraceutical`.
- `hasCommercialApplication()` — `DyeSource`, `FragranceSource`,
  `FlavorSource`, `FiberSource`, `InsecticideSource`, `IndustrialFeedstock`.
- `isToxicToMammals()` — `HumanToxin` or `LivestockToxin`. Drives safety
  constraints in higher application modules.
- `isPresentIn(PlantTissue)`, `isInduced()`, `isDevelopmental()` — tissue
  and induction queries.

When `PhytochemicalRole` gains a new permit, update the relevant axis-level
predicate to include it. The axis-level methods are the stable consumer
surface; new permits should never require call-site changes elsewhere.

## Two-axis classification of compounds

Plant compounds are classified along two orthogonal axes:

1. **Structural type** (chemistry domain) — carbon-skeleton taxonomy:
   indole alkaloid, monoterpene, flavonol, etc. This is a property of the
   molecule, not the plant. Modelled on `chemistry.CompoundInfo` as a
   required `StructuralType` (sealed interface in
   `chemistry.compound.structure`). The non-organic case is named
   explicitly via the `Element` and `Inorganic` permits rather than left
   to a null value. Top-level family membership is exposed through
   behavioral predicates on `Compound` (`isAlkaloid()`, `isTerpenoid()`,
   `isPhenolic()`, `isGlycoside()`, `isGlucosinolate()`).
2. **Ecological/use category** (plants domain) —
   `PhytochemicalCategory` enum on `PhytochemicalConstituent`. Coarser than
   structural type; bundles compounds by the bands a naturalist or
   phytochemistry textbook would group them under (alkaloid, terpenoid,
   glucosinolate, latex, …).

The two axes are deliberately complementary. A given compound carries one
structural type and appears under one ecological/use category per plant
record. The axes do not need to agree, and should not be flattened into
each other.

## Test fixtures

Record invariant tests live in `plants-api/src/test/java/`, one per record,
following the `Observer` → `MethodObserver` → `InvariantObservation` pattern
described in `kernels/CLAUDE.md`: `PlantTest`, `PlantFamilyTest`,
`PlantGenusTest`, `CultivarTest`, `SeedLineageTest`, `ProvenanceTest`,
`PlantProgramTest`, `PhytochemicalConstituentTest`.

Repository contract tests live in `plants-repository-test/`. Mocks are
package-private — the test contexts, mock tests, and `plants-core` tests that
construct them all live in the same package by design:

- `PlantSpeciesEntityRepositoryTest`, `PlantSpeciesEntityRepositoryMock`, `PlantSpeciesEntityRepositoryMockTest`
- `PlantFamilyEntityRepositoryTest`, `PlantFamilyEntityRepositoryMock`, `PlantFamilyEntityRepositoryMockTest`
- `PlantGenusEntityRepositoryTest`, `PlantGenusEntityRepositoryMock`, `PlantGenusEntityRepositoryMockTest`
- `CultivarEntityRepositoryTest`, `CultivarEntityRepositoryMock`, `CultivarEntityRepositoryMockTest`
- `SeedLineageEntityRepositoryTest`, `SeedLineageEntityRepositoryMock`, `SeedLineageEntityRepositoryMockTest`
- `PlantProgramEntityRepositoryTest`, `PlantProgramEntityRepositoryMock`, `PlantProgramEntityRepositoryMockTest`
- `PhytochemicalConstituentEntityRepositoryTest`, `PhytochemicalConstituentEntityRepositoryMock`,
  `PhytochemicalConstituentEntityRepositoryMockTest`

When adding a new entity to the domain, scaffold via the standard skills
(`/test-entity-source`, `/entity-repository`, `/entity-query`) — see
`domains/CLAUDE.md` for the full conventions.

## Data Extraction Pattern (plants catalogs)

Authoritative catalogs live under
`plants-repository-test/src/main/resources/plants/`, each loaded by its
corresponding `NamedTestEntitySource` at test time. The resource sub-directory
mirrors the Java sub-package.

| Catalog                    | Path                                                    | Loaded by                                  |
|----------------------------|---------------------------------------------------------|--------------------------------------------|
| Plant families                   | `plants/plant-families.json`                            | `PlantFamilyTestEntitySource`              |
| Plant genera                     | `plants/plant-genera.json`                              | `PlantGenusTestEntitySource`               |
| Plants                     | `plants/plant-species.json`                                    | `PlantSpeciesTestEntitySource`                    |
| Cultivars                  | `plants/cultivar/cultivars.json`                        | `CultivarTestEntitySource`                 |
| Seed lineages              | `plants/heritage/seed-lineages.json`                    | `SeedLineageTestEntitySource`              |
| Plant programs                   | `plants/management/plant-programs.json`                 | `PlantProgramTestEntitySource`             |
| Phytochemical constituents | `plants/phytochemistry/phytochemical-constituents.json` | `PhytochemicalConstituentTestEntitySource` |

### `plant-families.json`

Each entry must include:

- `"name": "<family-slug>"` — the `PlantFamilyName` natural key, the lowercased
  family epithet (`"lamiaceae"`, `"aristolochiaceae"`)
- `"order": "<Order>"`, `"family": "<Family>"` — `TaxonomicOrder` /
  `TaxonomicFamily` epithets, capitalised as in the Linnaean literature
- `"description": { ... }` — Durrell four-level `Description`
- `"commonNames": [ ... ]` — `Set<CommonName>`; `[]` means no asserted
  vernacular name yet

### `plant-genera.json`

Each entry must include:

- `"name": "<genus-slug>"` — the `PlantGenusName` natural key, the lowercased
  genus epithet (`"thymus"`, `"salvia"`)
- `"familyName": "<family-slug>"` — typed upward FK to a `PlantFamily`,
  enforced by `PlantGenusTestEntitySource`
- `"order"`, `"family"`, `"genus"` — locally carried epithets; `family` must
  match the resolved parent family's epithet
- `"description": { ... }`, `"commonNames": [ ... ]` — as above

### `plants.json`

Each entry must include:

- `"name": "<plant-slug>"` — the `PlantSpeciesName` natural key (no `id` field; ADR-022).
  Normally the lowercased binomial (`"aristolochia-californica"`). Five entries
  currently use a common or bare-genus name instead and duplicate a
  `PlantGenus` record; see the consistency plan.
- `"taxonomy": { "order", "family", "genus", "species" }` — `TaxonomicClassification`
- `"description": { "preschool", "elementary", "secondary", "university" }` — full
  Durrell four-level `Description`
- `"roles": [ ... ]` — `Set<PlantRole>` by enum constant name
- `"lifeForm": "<enum>"` — `PlantLifeForm` constant
- `"nativeBioregions": [ "<bioregion-slug>", ... ]` — `Set<Bioregion>` by slug
  (empty array `[]` means no asserted native range, never null)

### `cultivar/cultivars.json`

Each entry must include:

- `"name": "<cultivar-slug>"` — `CultivarName` natural key
- `"plantName": "<plant-slug>"` — soft FK to a `PlantSpecies` in `plants.json`
- `"commonName": "<display name>"` — non-blank human-readable name
- `"description": { ... }` — Durrell four-level `Description`
- `"varietyType": "<enum>"` — `OPEN_POLLINATED | HYBRID_F1 | HYBRID_F2 | UNKNOWN`
- `"fruitType": "<enum>"` — `PASTE | CHERRY | SLICER | BEEFSTEAK`
- `"seedSavingPolicy": "<enum>"` — `SAVE_ANNUALLY | DO_NOT_SAVE | CONDITIONAL | NOT_APPLICABLE`
- `"seedSource": <string | null>` — provenance note, nullable
- `"gardenNotes": <string | null>` — operational notes, nullable

### `heritage/seed-lineages.json`

Each entry must include:

- `"name": "<lineage-slug>"` — `SeedLineageName` natural key
- `"cultivarName": "<cultivar-slug>"` — soft FK to a `Cultivar`. Referenced
  cultivar must be `OPEN_POLLINATED` (service-layer rule, not record invariant)
- `"provenance": { "originator", "originLocation", "estimatedGenerations", "sourceNotes" }` —
  `Provenance` ValueObject; `sourceNotes` nullable
- `"description": { ... }` — Durrell four-level `Description`
- `"adaptationStartYear": <int>` — `0` means no active adaptation program
- `"selectionCriteria": <string | null>` — nullable
- `"notes": <string | null>` — nullable

### `phytochemistry/phytochemical-constituents.json`

Each entry must include:

- `"name": "<constituent-slug>"` — `PhytochemicalConstituentName` natural key.
  Encodes both sides of the link: `<plant-slug>-<compound-slug>` (e.g.
  `"aristolochia-californica-aristolochic-acid-i"`,
  `"creeping-thyme-thymol"`) — built programmatically via
  `PhytochemicalConstituentName.of(plantName, compoundName)` in code.
- `"plantName": "<plant-slug>"` — soft FK to a `PlantSpecies` in `plants.json`
- `"compoundName": "<compound-slug>"` — cross-domain soft FK to a
  `chemistry.Compound` in the chemistry catalog. Service-layer rule: the
  referenced compound must exist in `compounds-base.json` /
  `compounds-aristolochic-acid.json`. Not enforced in record invariants
  (cross-aggregate validation).
- `"description": { ... }` — Durrell four-level `Description` of the
  constituent in this plant: what the compound is, how the plant uses it,
  what makes it interesting in this species
- `"category": "<enum>"` — `PhytochemicalCategory` constant. Aligns with
  `chemistry.CompoundCategory` where the two enums overlap; expresses the
  ecological/use bucket from a naturalist's perspective
- `"roles": [ ... ]` — non-empty set of `PhytochemicalRole` permits using
  the discriminated `{"kind": "<NAME>"}` form. Multiple roles per
  constituent are common — caffeine is at once an `INSECT_DETERRENT`, a
  `PHARMACEUTICAL`, and a `NUTRACEUTICAL`. An empty array is an invariant
  violation
- `"tissues": [ ... ]` — non-empty set of `PlantTissue` enum values. Same
  compound in seed vs leaf vs latex carries different ecological meaning;
  unknown tissue is recorded as `["WHOLE_PLANT"]` rather than an empty array
- `"induction": "<enum>"` — `CONSTITUTIVE | INDUCED | DEVELOPMENTAL`. A
  compound expressed in two modes (e.g. low constitutive plus an induced
  burst on damage) is recorded as two separate constituents
- `"notes": <string | null>` — operational guidance: extraction notes,
  seasonal concentration variation, observation cadence; nullable

### `management/plant-programs.json`

Each entry must include:

- `"name": "<program-slug>"` — `PlantProgramName` natural key. Name the program
  after the *activity*, not the plant (e.g. `"pipevine-pesticide-exclusion"`,
  `"pipevine-larval-monitoring"`) — one plant may carry multiple programs
- `"plantName": "<plant-slug>"` — soft FK to a `PlantSpecies`
- `"description": { ... }` — Durrell four-level `Description` of the program itself
- `"constraint": <string | null>` — non-negotiable rule surfaced by
  PestManagement; nullable for pure-schedule programs
- `"notes": <string | null>` — operational guidance: schedules, observation
  cadence, application windows; nullable

### Conventions across all catalogs

- No `id` field on any record — domain records carry no `PersistenceId` (ADR-022).
- JSON field names match record component names exactly. Do not rename.
- Enum values serialize by constant name (`"VINE"`, `"KEYSTONE_HOST"`,
  `"OPEN_POLLINATED"`).
- Cross-domain references are slug strings, never numeric IDs.
- Catalogs are real Oak Vista data — failing tests indicate a domain model
  error or a real-world change, not bad fixtures.

## Pointers

- Domain-shared conventions: [`../CLAUDE.md`](../CLAUDE.md)
- Narrative / ecological context, Oak Vista lineup, BER, heritage program,
  open design questions: [`docs/briefings/plants-domain.md`](../../docs/briefings/plants-domain.md)
