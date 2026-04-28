# Plants Domain

Coding conventions and structural rules for the plants domain. Narrative,
ecological, and historical context lives in
[`plants-chat-briefing.md`](plants-chat-briefing.md) — load that file when the
conversation is about the *what* and *why* rather than the *how*.

## Sub-context layout

```
plants-api/com/naturalist/plants/
  Plant, PlantLifeForm, PlantRole, PlantRepository      — top-level botanical record
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

| Type | Location | Identity | Identifier class |
|------|----------|----------|------------------|
| `Plant` | `plants/` | `NamedEntity<PlantName>` | `identifiers/.../plants/PlantName` |
| `Cultivar` | `cultivar/` | `NamedEntity<CultivarName>` | `identifiers/.../plants/cultivar/CultivarName` |
| `SeedLineage` | `heritage/` | `NamedEntity<SeedLineageName>` | `identifiers/.../plants/heritage/SeedLineageName` |
| `Provenance` | `heritage/` | `ValueObject` | — |
| `PlantProgram` | `management/` | `NamedEntity<PlantProgramName>` | `identifiers/.../plants/management/PlantProgramName` |
| `PhytochemicalConstituent` | `phytochemistry/` | `NamedEntity<PhytochemicalConstituentName>` | `identifiers/.../plants/phytochemistry/PhytochemicalConstituentName` |

No `Aggregate` or `Entity<UUIDv7>` records in this domain yet.

## Soft FK chain

All cross-entity references are by `EntityName` slug — no compile-time coupling
between sub-contexts beyond shared identifier classes.

```
PlantProgram.plantName              → Plant.name        (PlantName)
Cultivar.plantName                  → Plant.name        (PlantName)
SeedLineage.cultivarName            → Cultivar.name     (CultivarName)
PhytochemicalConstituent.plantName  → Plant.name        (PlantName)
PhytochemicalConstituent.compoundName → chemistry Compound.name (CompoundName)
Plant.nativeBioregions              → kernels/biogeography Bioregion
```

The `PhytochemicalConstituent.compoundName` reference is the only soft FK
in this domain that crosses *domain* boundaries (rather than just sub-context
boundaries within plants). The reference is via `CompoundName` from the
`identifiers` module — `plants-api` does **not** depend on `chemistry-api`.
Cross-domain referential integrity (the referenced compound exists in the
chemistry catalog) is a service-layer rule, not a record invariant.

## Repository namespace pattern

Two sub-contexts currently expose a repository, both following the namespace
class convention from `domains/CLAUDE.md`:

- `PlantRepository` (top-level package-private class) →
  `protected interface PlantEntityRepository extends EntityRepository<PlantName, Plant>`
- `PlantProgramRepository` (management package-private class) →
  `protected interface PlantProgramEntityRepository extends EntityRepository<PlantProgramName, PlantProgram>`

`Cultivar` and `SeedLineage` do not yet have repositories. When added, follow
the same namespace pattern (`CultivarRepository` in `cultivar/`,
`SeedLineageRepository` in `heritage/`). N=1 per sub-context, so the repository
namespace collapses to a single nested interface in each.

No `PlantQuery`, `PlantEntityCollections`, or aggregate factories exist yet.
Add them when read-side surface materializes — query interfaces public in api,
factories package-private concrete classes in `plants-core`.

## Domain-specific invariants

- `Plant` — `name`, `taxonomy`, `description`, `roles`, `lifeForm`,
  `nativeBioregions` are all required (non-null). An empty `nativeBioregions`
  set means *no asserted native range*, not *unknown*.
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

## Behavioral predicates on the Plant record

These are first-class API methods, not inline `roles.contains(...)` checks at
call sites:

- `Plant.isKeystoneHost()` — `roles.contains(PlantRole.KEYSTONE_HOST)`. Drives
  zero-pesticide constraints in the PestManagement application module.
- `Plant.supportsBiocontrolInsects()` — `roles.contains(BENEFICIAL_INSECT_HABITAT)`.
- `Plant.isNitrogenFixer()` — `roles.contains(NITROGEN_FIXER)`.
- `Plant.isNativeTo(Bioregion)` — membership check on `nativeBioregions`.

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

Repository contract tests live in `plants-repository-test/`:

- `PlantEntityRepositoryTest`, `PlantEntityRepositoryMock`, `PlantEntityRepositoryMockTest`
- `PlantProgramEntityRepositoryTest`, `PlantProgramEntityRepositoryMock`, `PlantProgramEntityRepositoryMockTest`

When adding a new entity to the domain, scaffold via the standard skills
(`/test-entity-source`, `/entity-repository`, `/entity-query`) — see
`domains/CLAUDE.md` for the full conventions.

## Data Extraction Pattern (plants catalogs)

Authoritative catalogs live under
`plants-repository-test/src/main/resources/plants/`, each loaded by its
corresponding `NamedTestEntitySource` at test time. The resource sub-directory
mirrors the Java sub-package.

| Catalog | Path | Loaded by |
|---------|------|-----------|
| Plants | `plants/plants.json` | `PlantTestEntitySource` |
| Cultivars | `plants/cultivar/cultivars.json` | `CultivarTestEntitySource` |
| Seed lineages | `plants/heritage/seed-lineages.json` | `SeedLineageTestEntitySource` |
| Plant programs | `plants/management/plant-programs.json` | `PlantProgramTestEntitySource` |

### `plants.json`

Each entry must include:

- `"name": "<plant-slug>"` — the `PlantName` natural key (no `id` field; ADR-022)
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
- `"plantName": "<plant-slug>"` — soft FK to a `Plant` in `plants.json`
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

### `management/plant-programs.json`

Each entry must include:

- `"name": "<program-slug>"` — `PlantProgramName` natural key. Name the program
  after the *activity*, not the plant (e.g. `"pipevine-pesticide-exclusion"`,
  `"pipevine-larval-monitoring"`) — one plant may carry multiple programs
- `"plantName": "<plant-slug>"` — soft FK to a `Plant`
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
  open design questions: [`plants-chat-briefing.md`](plants-chat-briefing.md)
