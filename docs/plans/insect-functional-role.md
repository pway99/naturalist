# InsectFunctionalRole — Cross-Rank Functional Ecology

> **Status:** drafted 2026-05-19, immediately after Path A closed. Resolves
> PL-11.

**Goal.** Restore structured, query-addressable functional ecology
(`guilds: Set<FunctionalGuild>`, `beneficial: boolean`) as a cross-rank
concern. Today the structured assignment lives only on `InsectSpecies` —
the eight PL-2-migrated genus / family records (chrysoperla, halictus,
andrena, syrphidae, carabidae, tipulidae, hesperiidae, empoasca) silently
lost it when their under-identified species records were deleted, and
new genus / family-rank organisms have no home for it at all. Narrative
versions survive (`*-adult.ecologicalRole` text, genus description
tiers), but the enum-tagged path that `species().getByFunctionalGuild(...)`
uses can't reach them.

After this slice lands, `InsectFunctionalRole` is a new `Entity` keyed by
`InsectRankName` (mirroring the cross-rank pattern the
`insect-image-parent-rank` slice established for `InsectImage`). One
repository, one JSON catalog, one query: `getByGuild(POLLINATOR)`
returns every rank-record carrying that guild assignment regardless of
whether it's a family, genus, species, or subspecies.

---

## Scope decisions

### Why a separate entity, not fields on each rank record

Three options were weighed when PL-11 was raised. Option B
(separate cross-rank entity) was chosen because:

1. **Reuse of the cross-rank pattern.** `InsectImage` already uses
   `parentName: InsectRankName` + rank-aware FK constraints to attach to
   any of the four insect-side ranks. A second consumer of the same
   pattern (this) is the signal that the pattern is genuine and worth
   the modest entity scaffolding.
2. **Single query path.** `getByGuild(POLLINATOR)` returns all
   pollinators across ranks in one call. The status-quo expansion
   (`guilds` field on `InsectGenus` + `InsectFamily`) would force three
   parallel queries plus a union in the caller.
3. **Single source of truth.** With `guilds` on `InsectSpecies` *and*
   `guilds` on `InsectGenus` *and* `guilds` on `InsectFamily`, a species
   could be a POLLINATOR while its parent genus isn't marked, and the
   answer to "is this organism a pollinator?" depends on which rank
   record the caller checked. A separate entity puts the assignment
   exactly once per organism, with `parentName` as its only natural key.
4. **Decouples taxonomy from ecology.** A taxonomic record describes
   *what* the organism is; an ecological role assignment describes
   *what it does* in the garden. The two concerns evolve at different
   tempos — a genus's taxonomy is fixed once and never edited, while
   its ecological assignment can change as the Oak Vista census surfaces
   new data.

**Rejected — Option A (`guilds` + `beneficial` fields on `InsectGenus`
and `InsectFamily`).** Three parallel query paths; cross-rank queries
have to fan out manually; multiple sources of truth.

**Rejected — Option C (trait on a clade, like `MetabolyTrait`).**
Functional ecology isn't strictly phylogenetic. Carabidae has predator
and herbivore lineages; Apoidea has pollinators and kleptoparasites;
the same family can host organisms playing different roles. Clade-level
declarations would lose this nuance.

### `beneficial` paired with `guilds`

`InsectSpecies.beneficial` is a high-level garden-management flag — true
for species whose presence is desirable. It's conceptually tied to
`guilds` (a pollinator is generally beneficial; a pest generally isn't)
but isn't a strict function of the guild set — `MIGRATORY` species can
be either, and `FOOD_WEB` participants are usually neutral-to-negative.
Carrying both on the same record keeps them in lock-step and avoids
splitting the migration in two.

### `sightingNotes` stays out

`InsectSpecies.sightingNotes` is observation-event content ("Two
individuals photographed on crimson clover, April 2026; first pest
documented in Oak Vista census"). Belongs to the future sightings
entity that the identification roadmap will introduce — not to
functional-role. Out of scope for this slice. The two facts uniquely
lost on the deleted potato-leafhopper record are noted in PL-11 for
the sightings-entity slice when it arrives.

### `parentName` is `@EntityIdentifier`

One role assignment per organism record — `parentName` is unique within
`InsectFunctionalRoleTestEntitySource`. Multiple guilds per organism is
expressed via `Set<FunctionalGuild>`, not multiple role records.

### Entity vs NamedEntity

`InsectFunctionalRole` is a UUIDv7-keyed `Entity<InsectFunctionalRoleId>`,
not a `NamedEntity`. The natural key of an ecological-role assignment is
its parent reference, not a domain-specific slug — same shape as
`InsectImage`. The surrogate id is generated at construction; the
catalog refers to role assignments by parent slug, never by id.

---

## Architecture

```
domains/identifiers/src/main/java/com/naturalist/insects/
└── InsectFunctionalRoleId.java                — NEW EntityId (UUIDv7)

domains/insects/insects-api/src/main/java/com/naturalist/insects/
├── InsectFunctionalRole.java                  — NEW Entity record
├── InsectRepository.java                      — add FunctionalRoleRepository nested interface
├── InsectQuery.java                           — add FunctionalRoleQuery nested interface
├── InsectEntityCollections.java               — add FunctionalRoleCollection nested final class
└── InsectSpecies.java                         — Phase 4: remove guilds + beneficial fields

domains/insects/insects-api/src/test/java/com/naturalist/insects/
└── InsectFunctionalRoleTest.java              — NEW record invariants test

domains/insects/insects-core/src/main/java/com/naturalist/insects/
├── FunctionalRoleQueryImpl.java               — NEW query adapter
├── SpeciesQueryImpl.java                      — Phase 4: drop getByFunctionalGuild override
└── (no aggregate factory — role is not part of InsectAggregate)

domains/insects/insects-core/src/test/java/com/naturalist/insects/
└── FunctionalRoleQueryImplTest.java           — NEW query contract test

domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/
├── InsectFunctionalRoleTestEntitySource.java  — NEW TestEntitySource
└── InsectFunctionalRoleRepositoryMock.java    — NEW repository mock

domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/
├── InsectFunctionalRoleTestEntitySourceTest.java  — NEW
└── InsectFunctionalRoleRepositoryMockTest.java    — NEW behavioral contract test

domains/insects/insects-repository-test/src/main/resources/insects/
└── insect-functional-roles.json               — NEW catalog (16 records)

domains/identifiers-test/src/main/java/com/naturalist/insects/
└── TestInsectsIdentifiers.java                — add FunctionalRole identifiers

domains/insects/insects-console/src/main/java/com/naturalist/insects/console/
└── InsectsController.java                     — Phase 5: /insects/guild/{guild} fans across ranks

domains/insects/insects-repository-test/src/main/resources/insects/
└── insect-species.json                        — Phase 4: remove guilds + beneficial from each record
```

---

## Phases

This slice runs in four phases. Each is a coherent commit; the dependency
chain runs strictly forward (no phase depends on a later one). The full
slice is ~16 files; phase 1 alone is ~6 files and an empty-catalog smoke
test, so PR sizes stay reviewable.

### Phase 1 — Entity stack (no data yet)

Goal: the new entity exists, compiles, and has an empty JSON catalog
that loads cleanly. No data, no consumers wired.

- [ ] `InsectFunctionalRoleId` — UUIDv7 EntityId. Identical shape to
  `InsectImageId` (`of(UUID)`, `create()`).
- [ ] `InsectFunctionalRole` record — `Entity<InsectFunctionalRoleId>`
  with components `name`, `parentName : InsectRankName`,
  `guilds : Set<FunctionalGuild>`, `beneficial : boolean`. Field-level
  `@JsonTypeInfo` / `@JsonSubTypes` on `parentName` mirroring
  `InsectImage` exactly. Invariants: `entityId(name, …)`,
  `identifier(parentName, …)`, `notNull(guilds, …)` (empty set is
  valid — an organism can have *no* recorded guild yet),
  `notNull(beneficial, …)` is automatic via primitive boolean.
- [ ] `InsectFunctionalRoleTest` — record invariants test in
  `insects-api` test sources. Standard valid-case + invalid-case shape.
- [ ] `InsectRepository.FunctionalRoleRepository` —
  `protected interface` extending `EntityRepository<InsectFunctionalRoleId,
  InsectFunctionalRole>`. No domain-specific finder yet (added in
  Phase 3).
- [ ] `InsectEntityCollections.FunctionalRoleCollection` —
  `final class extends BehavioralCollection<InsectFunctionalRole>`.
- [ ] `InsectFunctionalRoleRepositoryMock` —
  `extends AbstractTestEntityRepository<...>` implementing
  `InsectRepository.FunctionalRoleRepository`.
- [ ] `InsectFunctionalRoleTestEntitySource` —
  `TestEntitySource<InsectFunctionalRoleId, InsectFunctionalRole>`.
  Three rank-aware `ForeignKeyConstraint`s for `parentName`
  (family / genus / species) mirroring `InsectImageTestEntitySource`.
  One `UniqueConstraint` on `parentName` (one role record per organism).
  `loadFile("insects/insect-functional-roles.json")` — file exists but
  empty: `[]`.
- [ ] `insect-functional-roles.json` — empty array `[]` to start.
- [ ] `InsectFunctionalRoleTestEntitySourceTest` extends
  `TestEntitySourceTest<...>` — the framework smoke test.
- [ ] `InsectFunctionalRoleRepositoryMockTest` —
  `NamedEntityRepositoryContractTest`-style hooks (but
  `EntityCommandContractTest`/`EntityQueryContractTest` for the
  surrogate-id branch). Identifier hooks point to two seeded records;
  this phase will need a couple of seed records (or defer the contract
  test to Phase 2 when records exist).
- [ ] `TestInsectsIdentifiers` — add `InsectFunctionalRole` scope with
  `NotFound.name` and (after Phase 2 seeds data) at least two known
  identifiers.
- [ ] User runs `mvn verify`. Commit.

**Defer to Phase 2 if contract tests need data:** the
`InsectFunctionalRoleRepositoryMockTest` may need at least two seed
records to satisfy `knownEntityNames()`. If so, defer that test class to
Phase 2.

### Phase 2 — Seed 16 records (8 species + 8 PL-2 ranks)

Goal: populate `insect-functional-roles.json` with the full back-fill.
Each record is generated by reading the source data and creating a
UUIDv7 + the `parentName` + `guilds` + `beneficial` triple.

Sources for back-fill:

| parentRank | parentName    | Source                                       |
|------------|---------------|----------------------------------------------|
| GENUS      | chrysoperla   | `e5e3c8d^` — green-lacewing species record   |
| GENUS      | halictus      | `be0bbce^` — native-sweat-bee species record |
| GENUS      | andrena       | `d12c3b2^` — grey-mining-bee species record  |
| FAMILY     | syrphidae     | `c3a3f8d^` — hoverfly species record         |
| FAMILY     | carabidae     | `c3a3f8d^` — ground-beetle species record    |
| FAMILY     | tipulidae     | `c3a3f8d^` — crane-fly species record        |
| FAMILY     | hesperiidae   | `c3a3f8d^` — skipper-butterfly species record |
| GENUS      | empoasca      | `05d430f^` — potato-leafhopper species record |
| SPECIES    | tachinid-fly        | current `insect-species.json`          |
| SPECIES    | braconid-wasp       | current `insect-species.json`          |
| SPECIES    | hippodamia-convergens | current `insect-species.json`        |
| SPECIES    | blattella-vaga      | current `insect-species.json`          |
| SPECIES    | xylocopa-varipuncta | current `insect-species.json`          |
| SPECIES    | vanessa-cardui      | current `insect-species.json`          |
| SPECIES    | battus-philenor     | current `insect-species.json`          |
| SPECIES    | colias-eurytheme    | current `insect-species.json`          |

Each record's JSON shape (matching `InsectImage`'s flat
EXTERNAL_PROPERTY form):

```json
{
  "name": "<uuidv7>",
  "parentRank": "GENUS",
  "parentName": "empoasca",
  "guilds": ["FOOD_WEB"],
  "beneficial": false
}
```

- [ ] Run a one-shot `git show <commit>:.../insect-species.json` for each
  of the four PL-2 commits, extract `guilds` + `beneficial` per organism,
  and assemble the eight cross-rank records.
- [ ] Read the eight species records' `guilds` + `beneficial` from the
  current `insect-species.json` (no git archaeology needed).
- [ ] Generate 16 UUIDv7 values (use `EntityId.newUUID()` via a one-shot
  jshell or test scratch class — same pattern as how earlier image-id
  catalog records were seeded).
- [ ] Write `insect-functional-roles.json` with the 16 records.
- [ ] `TestInsectsIdentifiers` — add identifier constants for at least
  two known records (e.g. `Empoasca.FunctionalRole.name`,
  `BattusPhilenor.FunctionalRole.name`) plus the
  `NotFound.functionalRoleName`.
- [ ] User runs `mvn verify`. Expect the loader to validate every record
  against the FK + unique-parent constraints. Commit.

### Phase 3 — Cross-rank query + console wiring

Goal: the new entity is queryable.

- [ ] `InsectQuery.FunctionalRoleQuery` — `interface FunctionalRoleQuery
  extends EntityQuery<InsectFunctionalRoleId, InsectFunctionalRole,
  FunctionalRoleCollection>`, with one domain finder:
  `FunctionalRoleCollection getByGuild(FunctionalGuild guild)`.
- [ ] `InsectQuery.functionalRoles()` accessor added to the namespace
  interface.
- [ ] `InsectRepository.FunctionalRoleRepository` —
  `List<InsectFunctionalRole> getByGuild(FunctionalGuild guild)`.
- [ ] `InsectFunctionalRoleRepositoryMock` — implements `getByGuild` via
  stream filter on `role.guilds().contains(guild)`.
- [ ] `FunctionalRoleQueryImpl` — adapter pattern: validate arguments
  via `observer().arguments("getByGuild", i -> i.notNull(guild,
  "guild"))`, delegate to repository.
- [ ] `FunctionalRoleQueryImplTest` — standard four-case
  pattern per ADR-002: null rejection, empty result, expected result,
  with-multiple-results.
- [ ] User runs `mvn verify`. Commit.

### Phase 4 — Remove `guilds` / `beneficial` from `InsectSpecies`

Goal: the species record stops carrying ecological-role fields.
`SpeciesQuery.getByFunctionalGuild` either delegates to the new query
or is removed entirely (callers move to
`insectQuery.functionalRoles().getByGuild(...)`).

- [ ] Audit all callers of `InsectSpecies.guilds()` and
  `InsectSpecies.beneficial()` — `grep -rn 'guilds()\|beneficial()'
  domains/insects --include='*.java'`. Each call site moves to looking
  up the role record via `functionalRoles()`, or — if the call site is
  display-only and species-specific — gets removed.
- [ ] Audit `SpeciesQuery.getByFunctionalGuild` callers. Option (a) drop
  the method entirely from `InsectQuery.SpeciesQuery` + its impl + its
  test. Option (b) keep it as a convenience that delegates to
  `functionalRoles().getByGuild(guild).stream().filter(r ->
  r.parentName() instanceof InsectSpeciesName).map(...).collect(...)`.
  **Recommend (a)** — fewer paths, single way to answer the question.
- [ ] Remove `guilds` and `beneficial` components from `InsectSpecies`
  record + invariants + their test assertions.
- [ ] Strip `guilds` and `beneficial` from every species record in
  `insect-species.json` (8 records).
- [ ] `InsectsController` — the `/insects/guild/{guild}` console page
  currently calls `species().getByFunctionalGuild(...)`. Switch to
  `functionalRoles().getByGuild(...)` and resolve each result's
  `parentName` to its source record (which may be a family / genus /
  species — so the resolved set is `Set<InsectRankName>` with mixed
  permits). Template adjustments may follow if the page assumes
  species-only.
- [ ] User runs `mvn verify`. Expect cross-cutting test changes in
  `SpeciesQueryImplTest`, `InsectSpeciesTest`, any console template
  tests for the guild page. Commit.

### Phase 5 — Roll work-tracker + parking-lot forward

- [ ] Remove the PL-11 entry from `notes/parking-lot.md`. (PL-11 was
  raised as a fork; the answer is now in code.)
- [ ] Update `work-tracker.md` — drop the PL-11 row from Active efforts,
  add the slice to Recently completed.
- [ ] Commit.

---

## Test plan

### New tests

- **`InsectFunctionalRoleTest`** — record invariants (valid case;
  invalid: null name, null parentName, null guilds, null commonNames).
- **`InsectFunctionalRoleTestEntitySourceTest`** —
  `TestEntitySourceTest<...>` smoke test.
- **`InsectFunctionalRoleRepositoryMockTest`** — entity-command +
  entity-query contract test per ADR-002.
- **`FunctionalRoleQueryImplTest`** — `getByGuild` four-case pattern
  (null rejection; guild with no records; guild with one record across
  one rank; guild with multiple records across multiple ranks).

### Modified tests

- **`InsectSpeciesTest`** — drop `guilds` / `beneficial` from valid + invalid
  shapes; drop the convenience-query unit tests.
- **`SpeciesQueryImplTest`** — drop `getByFunctionalGuild` cases if the
  method is removed.
- **`InsectsControllerTest` / template tests for `/guild/{guild}`** —
  switch expected fixtures to whatever the new cross-rank query returns.

### Verified-only

- `CladePlacementResolutionTest`, `ImageQueryImplTest`,
  `InsectAggregateFactoryTest` — should pass without change. Functional
  role is orthogonal to taxonomy + image + clade.

---

## Risks

- **Cross-rank console page redesign.** `/insects/guild/{guild}` today
  assumes a list of species; after Phase 4 it lists mixed ranks. The
  template either renders generically (display the `parentName.value()`
  + `parentName` class display name) or branches per-rank. The latter
  is more work but better UX. **Mitigation:** treat the template as
  display-only in Phase 4 and defer UX polish to a follow-up if it
  ramps; the data path is the important part.
- **UUIDv7 generation for seed data.** Sixteen ids need stable values
  in JSON. **Mitigation:** generate once via the kernel's
  `EntityId.newUUID()`, commit the file, never regenerate. Same pattern
  the image catalog uses.
- **`guilds` callers outside the insects domain.** A grep across the
  whole repo should catch them. `getByFunctionalGuild` is currently
  consumed only by the console controller, but verify before removing.

---

## Out of scope

- **`sightingNotes` migration.** Observation-event content;
  belongs to the future sightings entity (identification-roadmap
  Phase 1+). PL-11 notes the two facts uniquely lost on the deleted
  potato-leafhopper record for that future slice.
- **`beneficialProfile` migration.** `InsectSpecies` carries a richer
  optional `beneficialProfile` value object beyond the boolean
  `beneficial` flag. Out of scope here — the boolean is the
  query-addressable layer; `beneficialProfile` is descriptive and stays
  on `InsectSpecies` for now.
- **Plant / soil / other-domain analogues.** Plants have functional
  roles too (cover crop vs ornamental); not addressed here.

---

## Done when

- `mvn verify` is green at repo root.
- `insectQuery.functionalRoles().getByGuild(POLLINATOR)` returns the
  full set of pollinator records across all four ranks.
- `insectQuery.functionalRoles().getByGuild(FOOD_WEB)` returns the
  empoasca record (genus rank) along with any species-rank food-web
  participants — proving the cross-rank pattern works end-to-end.
- `InsectSpecies` no longer carries `guilds` or `beneficial`
  components.
- All 16 records in `insect-functional-roles.json` load cleanly;
  unique-parent constraint catches accidental duplication.
- PL-11 entry removed from parking lot; work-tracker reflects the
  closed slice.
