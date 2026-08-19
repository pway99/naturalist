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
  PlantOrder, PlantFamily, PlantGenus, PlantSpecies     — botanical rank records
  PlantEcologicalRole                                   — roles held at any rank
  GrowthHabit, LifeCycle, PlantRole                     — vocabularies
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
| `PlantOrder`               | `plants/`         | `NamedEntity<PlantOrderName>`               | `identifiers/.../plants/PlantOrderName`                              |
| `PlantFamily`              | `plants/`         | `NamedEntity<PlantFamilyName>`              | `identifiers/.../plants/PlantFamilyName`                             |
| `PlantGenus`               | `plants/`         | `NamedEntity<PlantGenusName>`               | `identifiers/.../plants/PlantGenusName`                              |
| `PlantSpecies`             | `plants/`         | `NamedEntity<PlantSpeciesName>`             | `identifiers/.../plants/PlantSpeciesName`                            |
| `PlantEcologicalRole`      | `plants/`         | `Entity<PlantEcologicalRoleId>`             | `identifiers/.../plants/PlantEcologicalRoleId`                       |
| `Cultivar`                 | `cultivar/`       | `NamedEntity<CultivarName>`                 | `identifiers/.../plants/cultivar/CultivarName`                       |
| `SeedLineage`              | `heritage/`       | `NamedEntity<SeedLineageName>`              | `identifiers/.../plants/heritage/SeedLineageName`                    |
| `Provenance`               | `heritage/`       | `ValueObject`                               | —                                                                    |
| `PlantProgram`             | `management/`     | `NamedEntity<PlantProgramName>`             | `identifiers/.../plants/management/PlantProgramName`                 |
| `PhytochemicalConstituent` | `phytochemistry/` | `NamedEntity<PhytochemicalConstituentName>` | `identifiers/.../plants/phytochemistry/PhytochemicalConstituentName` |

No `Aggregate` records in this domain yet. `PlantEcologicalRole` is the only
`Entity<UUIDv7>` — it has no natural key because it is keyed by *which taxon* it
describes, at whatever rank that taxon was catalogued.

The four rank names are permits of **`PlantRankName`** (sealed, in
`identifiers/.../plants/`), so anything that attaches to a plant — a role, a planting,
a photograph — can name the rank the evidence actually supported. `rank()` is total:
every permit answers with its `LinealRank`. Cultivar and crop type are *not* permits;
they are orthogonal axes, not rungs. See
[`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md)
section D for the test that distinguishes the two.

## Soft FK chain

All cross-entity references are by `EntityName` slug — no compile-time coupling
between sub-contexts beyond shared identifier classes.

```
PlantFamily.orderName                 → PlantOrder.name   (PlantOrderName)
PlantGenus.familyName                 → PlantFamily.name  (PlantFamilyName)
PlantSpecies.genusName                → PlantGenus.name   (PlantGenusName)
PlantEcologicalRole.plantName         → any rank record   (PlantRankName)
PlantProgram.plantName                → any rank record   (PlantRankName)
PhytochemicalConstituent.plantName    → any rank record   (PlantRankName)
Cultivar.plantName                    → PlantSpecies.name (PlantSpeciesName)
SeedLineage.cultivarName              → Cultivar.name     (CultivarName)
PhytochemicalConstituent.compoundName → chemistry Compound.name (CompoundName)
PlantSpecies.nativeBioregions         → kernels/biogeography Bioregion
```

**The rank chain is closed and enforced.** Order → family → genus → species is a
typed upward FK at every rung, and each rank's `TestEntitySource` declares a
`ForeignKeyConstraint` against its parent, so a fixture whose parent is missing
fails at load rather than at some later query. `PlantOrder` is the top and declares
none.

**Clade placement is an axis, and it attaches at `PlantOrder` only.** `PlantOrder`
carries `@Nullable Clade placedIn` (`kernels/clades`) locating the order in the
rank-free phylogenetic tree (angiosperms → magnoliids / monocots / eudicots → …).
This is a **stated deviation from insects**, which carries `placedIn` on every rank:
every mainstream botanical clade node is *supra-ordinal* (above Order in APG IV), so
a family, genus, or species resolves its clade transitively by walking up to its
order — a per-rank `placedIn` would only replicate the order's value and invite drift.
No `ForeignKeyConstraint`: integrity is type-level, since `Clade.of(slug)` throws on an
unknown slug at fixture load, and `placedIn` is nullable-by-design (an order of genuinely
uncertain placement carries `null`, never a fabricated node). There is **no
plants-owned trait function** — plants declares no clade trait, so clade is pure
placement, not inheritance. There is deliberately **no `PlantClass` rank**; supra-ordinal
structure is clade content, never a fifth rung. `plants-api` depends on `clades` for this;
it does **not** depend on `insects-api`. Design: `docs/plans/2026-08-16-plants-clades-design.md`.

**Three references attach at a rank rather than a specific one** —
`PlantEcologicalRole.plantName`, `PlantProgram.plantName`, and
`PhytochemicalConstituent.plantName` are all typed `PlantRankName` (a role, a
management program, or a compound can be asserted of a genus as readily as a species;
`thymus-thymol` is a genus-level constituent). None can declare a
`ForeignKeyConstraint`: the fixture loader resolves a single source class, and these
span four. Each carries a `@JsonSubTypes` dispatch on a sibling `plantRank`
discriminator, and its integrity is covered by a catalog-data test
(`PlantEcologicalRoleCatalogDataTest`, `PlantProgramCatalogDataTest`,
`PhytochemicalConstituentCatalogDataTest`, all via the shared `PlantRankResolution`
helper) instead of a declarative FK. That is the cost of cross-rank attachment until
the loader learns about sealed FK targets.

The `PhytochemicalConstituent.compoundName` reference is the only soft FK
in this domain that crosses *domain* boundaries (rather than just sub-context
boundaries within plants). The reference is via `CompoundName` from the
`identifiers` module — `plants-api` does **not** depend on `chemistry-api`.
Cross-domain referential integrity (the referenced compound exists in the
chemistry catalog) is a service-layer rule, not a record invariant.

## Repository and query namespaces

The **top-level plants namespace** (N>1: five rank/role entities) keeps its namespace
shape; the **four single-entity sub-contexts** are N=1-collapsed (ADR-020, M5).

- `PlantRepository` (top-level package-private class) → `SpeciesRepository`,
  `OrderRepository`, `FamilyRepository`, `GenusRepository`, `EcologicalRoleRepository`
- `CultivarRepository`, `SeedLineageRepository`, `PlantProgramRepository`,
  `PhytochemicalConstituentRepository` — each a **top-level package-private interface**
  (no wrapping class, no nested `*EntityRepository`); the sub-context holds one entity.

Read side, all public in api, adapters in `plants-core`:

- `PlantQuery` → `species()` (+ `forGenusName`, `forFamilyName`), `orders()`,
  `families()` (+ `forOrderName`), `genera()` (+ `forFamilyName`),
  `ecologicalRoles()` (+ `forPlantName`)
- `CultivarQuery` **is** the cultivar query — `extends EntityQuery<…>` with `forPlantName`
  inline; no `cultivars()` accessor, no nested type.
- `SeedLineageQuery` (+ `forCultivarName`), `PlantProgramQuery` (+ `forPlantName`),
  `PhytochemicalConstituentQuery` (+ `forPlantName`, `forCompoundName`) — same collapsed
  shape.

Collections: the top-level `PlantEntityCollections` namespace (`SpeciesCollection`,
`OrderCollection`, `FamilyCollection`, `GenusCollection`, `EcologicalRoleCollection`);
each sub-context has a **top-level** `<Entity>Collection` (no `*EntityCollections`
namespace). No aggregate factories exist yet.

The top-level plants namespace follows ADR-020 (M4, 2026-08-16): nested types drop the
domain prefix and `Entity` infix (`FamilyQuery`, `GenusRepository`, `SpeciesCollection`),
species accessor `species()`, matching insects. The four single-entity sub-contexts are
N=1-collapsed (M5, 2026-08-16).

**One deliberate deviation from the M5 plan:** the four `<Entity>TestContext` factories
were *not* folded into `PlantsTestContext`. They live in their sub-context packages to
reach the package-private `*QueryImpl`/`*RepositoryMock`; the root-package
`PlantsTestContext` cannot (insects avoids this only because its impls share the root
package). Each `*TestContext` was simplified to `new <Entity>QueryImpl(new <Entity>RepositoryMock(db))`.

## Domain-specific invariants

Every record below has an invariant test in `plants-api/src/test/java` covering
the valid shape, the all-null shape, and any domain-meaningful edge (empty role
set, blank provenance, nullable-by-design fields).

- `PlantOrder` — `name`, `order`, `description`, `commonNames` required. The top
  of the chain, so no upward FK. An empty `commonNames` set means *no asserted
  vernacular name yet*. `placedIn` (`@Nullable Clade`, `kernels/clades`) is a
  nullable-by-design axis, not an invariant — see the clade-axis note below.
- `PlantFamily` — `name`, `orderName`, `family`, `description`, `commonNames`
  required.
- `PlantGenus` — `name`, `familyName`, `family`, `genus`, `description`,
  `commonNames` required. The redundant `family` epithet is carried locally so a
  catalog-assembly chain check does not have to resolve the parent record. The
  order is deliberately *not* carried — two rungs up is a copy, not a chain check.
- `PlantSpecies` — `name`, `genusName`, `epithet`, `description`, `growthHabit`,
  `lifeCycle`, `nativeBioregions`, `commonNames` required (non-null). An empty
  `nativeBioregions` set means *no asserted native range*, not *unknown*. A
  species record is the bottom rung and nothing else: it holds no roles (those
  live on `PlantEcologicalRole`) and no taxonomy string. `growthHabit`
  (structure: `GrowthHabit`, USDA vocabulary) and `lifeCycle` (duration:
  `LifeCycle`) are two orthogonal axes — a vine may be annual or perennial — split
  from the former single `PlantLifeForm` in M6. Both stay on the species rather
  than moving up because a genus spans them.
- `PlantEcologicalRole` — `id`, `plantName`, `roles` required; `roles` non-empty.
  `plantName` is a `PlantRankName`, so a role can be asserted of a genus when the
  evidence stops there.
- `Cultivar` — `name`, `plantName`, `description`, `varietyType`, `fruitType`,
  `seedSavingPolicy` required. `commonName` non-blank. `seedSource` and
  `gardenNotes` nullable.
- `SeedLineage` — `name`, `cultivarName`, `provenance`, `description`
  required. **Service-layer rule:** the referenced cultivar must be
  `VarietyType.OPEN_POLLINATED`. Not enforced in record invariants (cross-entity).
- `PlantProgram` — `name`, `plantName`, `description` required. `plantName` is a
  `PlantRankName`, so a program can target a genus. `constraint`
  and `notes` nullable; `hasConstraint()` predicate distinguishes the two
  program shapes (constraint-bearing vs pure schedule).
- `PhytochemicalConstituent` — `name`, `plantName`, `compoundName`,
  `description`, `category`, `induction` required (non-null). `plantName` is a
  `PlantRankName` — a compound can be recorded for a genus (`thymus-thymol`). `roles` and
  `tissues` are non-empty sets — a constituent with no role is data without
  a story; an unknown tissue should be recorded as `WHOLE_PLANT` rather than
  an empty set. `notes` nullable. **Service-layer rule:** the referenced
  `compoundName` must exist in the chemistry catalog (cross-aggregate, not
  enforced in record invariants).
- `Provenance` (ValueObject) — `originator`, `originLocation` non-blank.

## Behavioral predicates on the ecological-role record

These are first-class API methods, not inline `roles.contains(...)` checks at
call sites:

- `PlantEcologicalRole.isKeystoneHost()` — `roles.contains(PlantRole.KEYSTONE_HOST)`.
- `PlantEcologicalRole.supportsBiocontrolInsects()` — `roles.contains(BENEFICIAL_INSECT_HABITAT)`.
- `PlantEcologicalRole.isNitrogenFixer()` — `roles.contains(NITROGEN_FIXER)`.
- `PlantEcologicalRole.playsRole(PlantRole)` — generic membership check.
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
described in `kernels/CLAUDE.md`: `PlantSpeciesTest`, `PlantOrderTest`,
`PlantFamilyTest`, `PlantGenusTest`, `PlantEcologicalRoleTest`, `CultivarTest`, `SeedLineageTest`, `ProvenanceTest`,
`PlantProgramTest`, `PhytochemicalConstituentTest`.

Repository contract tests live in `plants-repository-test/`. Mocks are
package-private — the test contexts, mock tests, and `plants-core` tests that
construct them all live in the same package by design:

- `PlantSpeciesRepositoryTest`, `PlantSpeciesRepositoryMock`, `PlantSpeciesRepositoryMockTest`
- `PlantOrderRepositoryTest`, `PlantOrderRepositoryMock`, `PlantOrderRepositoryMockTest`
- `PlantFamilyRepositoryTest`, `PlantFamilyRepositoryMock`, `PlantFamilyRepositoryMockTest`
- `PlantGenusRepositoryTest`, `PlantGenusRepositoryMock`, `PlantGenusRepositoryMockTest`
- `PlantCultivarRepositoryMock`, `PlantCultivarRepositoryMockTest`
- `PlantSeedLineageRepositoryMock`, `PlantSeedLineageRepositoryMockTest`
- `PlantProgramRepositoryMock`, `PlantProgramRepositoryMockTest`
- `PlantPhytochemicalConstituentRepositoryMock`, `PlantPhytochemicalConstituentRepositoryMockTest`

(Standalone concrete adapters carry the domain prefix and drop the "Entity" infix
per ADR-020 §5; nested ports stay bare.)

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
| Plant orders               | `plants/plant-orders.json`                              | `PlantOrderTestEntitySource`               |
| Plant families             | `plants/plant-families.json`                            | `PlantFamilyTestEntitySource`              |
| Plant genera               | `plants/plant-genera.json`                              | `PlantGenusTestEntitySource`               |
| Plant species              | `plants/plant-species.json`                             | `PlantSpeciesTestEntitySource`             |
| Plant ecological roles     | `plants/plant-ecological-roles.json`                    | `PlantEcologicalRoleTestEntitySource`      |
| Cultivars                  | `plants/cultivar/cultivars.json`                        | `CultivarTestEntitySource`                 |
| Seed lineages              | `plants/heritage/seed-lineages.json`                    | `SeedLineageTestEntitySource`              |
| Plant programs                   | `plants/management/plant-programs.json`                 | `PlantProgramTestEntitySource`             |
| Phytochemical constituents | `plants/phytochemistry/phytochemical-constituents.json` | `PhytochemicalConstituentTestEntitySource` |

### `plant-orders.json`

Each entry must include:

- `"name": "<order-slug>"` — the `PlantOrderName` natural key, the lowercased
  order epithet (`"lamiales"`, `"piperales"`)
- `"order": "<Order>"` — `TaxonomicOrder` epithet, capitalised as in the
  Linnaean literature
- `"placedIn": "<clade-slug>"` — the most-specific `kernels/clades` node for this
  order (`"magnoliids"`, `"lamiids"`, `"fabids"`, …). Loaded via `Clade.of(slug)`;
  an unknown slug fails at fixture load. Omit the field (or `null`) only when the
  APG IV placement is genuinely uncertain — never fabricate a node
- `"description": { ... }`, `"commonNames": [ ... ]` — as below

### `plant-families.json`

Each entry must include:

- `"name": "<family-slug>"` — the `PlantFamilyName` natural key, the lowercased
  family epithet (`"lamiaceae"`, `"aristolochiaceae"`)
- `"orderName": "<order-slug>"` — typed upward FK to a `PlantOrder`, enforced by
  `PlantFamilyTestEntitySource`
- `"family": "<Family>"` — `TaxonomicFamily` epithet, capitalised as in the
  Linnaean literature
- `"description": { ... }` — Durrell four-level `Description`
- `"commonNames": [ ... ]` — `Set<CommonName>`; `[]` means no asserted
  vernacular name yet

### `plant-genera.json`

Each entry must include:

- `"name": "<genus-slug>"` — the `PlantGenusName` natural key, the lowercased
  genus epithet (`"thymus"`, `"salvia"`)
- `"familyName": "<family-slug>"` — typed upward FK to a `PlantFamily`,
  enforced by `PlantGenusTestEntitySource`
- `"family"`, `"genus"` — locally carried epithets; `family` must match the
  resolved parent family's epithet. No `"order"` — it is two rungs up
- `"description": { ... }`, `"commonNames": [ ... ]` — as above

### `plant-species.json`

Each entry must include:

- `"name": "<plant-slug>"` — the `PlantSpeciesName` natural key (no `id` field;
  ADR-022), always the lowercased binomial (`"aristolochia-californica"`). A
  vernacular or bare-genus slug means the record belongs at genus rank instead —
  the 2026-08-16 rank audit demoted five of them.
- `"genusName": "<genus-slug>"` — typed upward FK to a `PlantGenus`, enforced by
  `PlantSpeciesTestEntitySource`
- `"epithet": "<species>"` — `TaxonomicSpecies`, the species epithet alone
- `"description": { "preschool", "elementary", "secondary", "university" }` — full
  Durrell four-level `Description`
- `"growthHabit": "<enum>"` — `GrowthHabit` constant (TREE, SHRUB, SUBSHRUB,
  FORB_HERB, GRAMINOID, VINE)
- `"lifeCycle": "<enum>"` — `LifeCycle` constant (ANNUAL, BIENNIAL, PERENNIAL)
- `"nativeBioregions": [ "<bioregion-slug>", ... ]` — `Set<Bioregion>` by slug
  (empty array `[]` means no asserted native range, never null)

### `plant-ecological-roles.json`

Each entry must include:

- `"id": "<uuidv7>"` — `PlantEcologicalRoleId`
- `"plantRank": "<ORDER|FAMILY|GENUS|SPECIES>"` — the Jackson discriminator that
  selects which `PlantRankName` permit `plantName` deserializes as
- `"plantName": "<slug>"` — the taxon this role describes, at whichever rank
- `"roles": [ ... ]` — `Set<PlantRole>` by enum constant name, non-empty

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
  `"thymus-thymol"`) — built programmatically via
  `PhytochemicalConstituentName.of(plantName, compoundName)` in code.
- `"plantRank": "<ORDER|FAMILY|GENUS|SPECIES>"` — Jackson discriminator selecting
  which `PlantRankName` permit `plantName` deserializes as
- `"plantName": "<slug>"` — the taxon that produces this compound, at whichever rank
  (`thymus` is a genus-level example). Integrity covered by
  `PhytochemicalConstituentCatalogDataTest`, not a `ForeignKeyConstraint`
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
- `"plantRank": "<ORDER|FAMILY|GENUS|SPECIES>"` — Jackson discriminator selecting
  which `PlantRankName` permit `plantName` deserializes as
- `"plantName": "<slug>"` — the taxon the program targets, at whichever rank.
  Integrity covered by `PlantProgramCatalogDataTest`, not a `ForeignKeyConstraint`
- `"description": { ... }` — Durrell four-level `Description` of the program itself
- `"constraint": <string | null>` — non-negotiable rule the program enforces
  (e.g. a pesticide-exclusion window); nullable for pure-schedule programs
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
