# Soil Foundation (Slice 1) — Design

**Date:** 2026-08-09
**Status:** Design revised after framework investigation; user approved "full aggregate now".
Being implemented autonomously (2026-08-09) for later review.
**Program:** "Soil ↔ Zone navigation." Roadmap (spine only):
**Slice 1 — soil foundation (this doc)** → Slice 2 — zone foundation → Slice 3 — catalog wiring.
Rehab-trend analysis and fall-test ingestion are deferred to their own efforts.

## Problem

The soil domain is an **api-only skeleton**: `soil-api` carries the model (`SoilProfile`
aggregate, `SoilProfileInfo` root, `LabAnalysisInfo`, `NutrientPanel`, events/observations) but
there is **no core, no repository, no test-entity-source, and no test data**. Nothing is
wired. Every later capability sits on foundation that does not exist.

Two model-level facts, discovered during design, shape the build:

1. **`SoilProfile` is an `Aggregate`, and `Aggregate` is not `Named`.** The framework's
   `EntityRepository<NAME, ENTITY extends Named<NAME>>` and `EntityQuery` can only store /
   query `Named` types. Aggregates in this codebase are **never stored** — they are
   **assembled by a factory** from persisted `Named` parts and returned through a bespoke
   query (exactly how insects builds `InsectTaxonView` via `InsectTaxonViewFactory` +
   `TaxonViewQuery.getByName → Optional<InsectTaxonView>`). The `Named<SoilProfileName>` in
   this domain is **`SoilProfileInfo`** (the aggregate root entity).
2. **`NutrientPanel` can represent only ~9 of the ~20 chemistry lines an FGL report carries.**
   Fixtures use real measurements (`domains/CLAUDE.md`), so the panel must grow before a
   `LabAnalysisInfo` fixture can be faithful.

## Framework naming (authoritative — the CLAUDE.md prose is stale)

The docs say `NamedTestEntitySource` / `NamedEntityRepository` /
`NamedEntityRepositoryContractTest`. **Those types do not exist.** The real framework types:

| CLAUDE.md prose                        | Actual type (`com.naturalist.data`)                         |
|----------------------------------------|-------------------------------------------------------------|
| `NamedTestEntitySource<NAME,ENTITY>`   | `TestEntitySource<NAME, ENTITY extends Named<NAME>>`        |
| (its base test)                        | `TestEntitySourceTest<NAME, ENTITY, DS>` (requires **≥4** entities) |
| `NamedEntityRepository<NAME,ENTITY>`   | `EntityRepository<NAME, ENTITY extends Named<NAME>>`        |
| repo validation base                   | `AbstractEntityRepository<NAME, ENTITY>`                     |
| in-memory mock base                    | `AbstractTestEntityRepository<NAME, ENTITY, NTS>`           |
| `NamedEntityRepositoryContractTest`    | `EntityRepositoryTest<NAME, ENTITY>`                        |
| query port                             | `EntityQuery<NAME, E extends Named<NAME>, EC>`              |
| query adapter base                     | `AbstractEntityQuery<NAME, E, EC, R>`                       |
| query contract test                    | `EntityQueryContractTest<NAME, E, EC>`                      |

`EntityRepository` methods: `getByName`→`Optional`, `getByEntityNameSet`→`List`,
`getPage`→`Page`, `insert`/`update`→void, `save`→ENTITY. Mocks extend
`AbstractTestEntityRepository<NAME,ENTITY,NTS>` (3rd param = the concrete `TestEntitySource`),
carry `@DomainService`, take a single `NaturalistDatabase` ctor. `TestEntitySource` is only
ever instantiated by `NaturalistDatabase.getNamed(Class)` (reflective, one `(NaturalistDatabase)`
ctor).

## Scope

**In scope**
- **Model:** extend `NutrientPanel` to a faithful measured snapshot + add `CationBaseSaturation`
  (§A). Add a `soilProfileName` FK to `LabAnalysisInfo` (§ aggregate assembly).
- **Fixtures:** `TestSoilIdentifiers`; a `SoilProfileInfoTestEntitySource` (4 real Oak Vista
  profiles) and a `LabAnalysisInfoTestEntitySource` (the 2 real March analyses); JSON catalogs.
- **Persistence:** `SoilProfileInfoRepository` + `LabAnalysisInfoRepository` (each: interface,
  mock, behavioral contract, mock test).
- **Assembly + query:** `SoilProfileFactory` composing `SoilProfile` from `SoilProfileInfo` +
  its `LabAnalysisInfo` list (+ empty event lists, null mulch); `SoilProfileQuery.getBySoilProfileName
  → Optional<SoilProfile>`; the two child `EntityQuery`s that feed it; `SoilTestContext` +
  `SoilTestContextInternal`; `soil-test-context` module scaffolding.

**Out of scope (deferred, flagged)**
- **Event persistence** (amendment/irrigation/tillage/precipitation). The assembled
  `SoilProfile` carries **empty** event lists this slice; wiring event repositories is the
  existing soil/sensor backlog (`docs/notes/pending-implementation.md`).
- **The lab's derived/advisory lines** — SAR, lime/gypsum requirement, texture class, and the
  Fertilization Recommendations table. Measured chemistry only.
- **Zone wiring** (Slice 3), rehab-trend, fall-test ingestion, console surfacing — later.
- **`MulchLayer`** — assembled as `null` this slice (documented straw layer is a trivial later add).

## Key decisions

1. **The aggregate is assembled, not stored.** `SoilProfileInfo` (NamedEntity) and
   `LabAnalysisInfo` (Entity) are the persisted parts; `SoilProfileFactory` composes `SoilProfile`.
   The factory is a single package-private concrete class in `soil-core` — no interface, no
   `Impl` suffix, never declared in `soil-api` (ADR-020).
2. **`LabAnalysisInfo` gains a `soilProfileName` FK** (mirrors `InsectImage.parentName`), so the
   factory fetches a profile's analyses via `labAnalysisQuery.forSoilProfileName(name)`. This
   is the only child-model change; events are untouched (deferred).
3. **Faithful chemistry, not the full report** (user-selected). Add every measured value; add
   `CationBaseSaturation`; stop short of the lab's derived/advisory lines.
4. **`CationBaseSaturation` is one `ValueObject` per sample** — five CEC occupancy percentages
   (Ca/Mg/K/Na/H) that only mean anything collectively and sum to ~100% (ADR-013). Named to
   avoid collision with the existing physical `saturationPct`.
5. **The raw FGL PDF is source of truth** for measured values, optimum ranges, and status
   (read from the colored band, not recomputed — matters for micros). Where the interpretive
   report disagrees, the lab report wins. Optimum ranges are per-sample, so the lab-assigned
   `NutrientStatus` stays a stored field (not recomputed from a global table).
6. **Four profiles** (`box1`, `backyard-north`, `backyard-center`, `backyard-south`) — the four
   documented in `SoilProfileInfo`'s javadoc. Required anyway: `TestEntitySourceTest` asserts
   **≥4 entities**. `box1`→`-001`, the three backyard subzones→`-002` (they shared the sample).
7. **N=1 collapse per package.** `SoilProfileInfo` is the only `NamedEntity` in `com.naturalist.soil`;
   `LabAnalysisInfo` the only `Entity` in `com.naturalist.soil.observation`. Each gets a top-level
   package-private `<Entity>Repository` + top-level public query — no namespace wrappers
   (chemistry `Product`/`Element` template).

## Design

### A. Model extension — `NutrientPanel` → faithful chemistry

Add measured `NutrientReading` fields (`BigDecimal value` + lab `NutrientStatus`, lbs/1000 sqft):
`magnesiumExch`, `sodiumExch`, `sodiumSoluble`, `zinc`, `manganese`, `iron`, `copper`, `chloride`.

Add `CationBaseSaturation` (`ValueObject` record, top-level in the `observation` package) with
five `BigDecimal` components `calciumPct/magnesiumPct/potassiumPct/sodiumPct/hydrogenPct`;
`invariants()` = each `notNull` + `inRange(v, ZERO, 100)`. New `NutrientPanel` component
`cationBaseSaturation`. `NutrientReading` also gains a `notNull` + non-negative guard is *not*
required beyond current (leave `NutrientReading` as-is aside from being reused).

`NutrientPanel.invariants()` adds one `.valueObject(...)` per new reading + the base saturation.
Existing `saturationPct`, `cecMeqPer100g`, `pH`, `ecDsPerMeter`, `limestonePct` unchanged.

**Ripple:** the only `new NutrientPanel/SoilProfile/LabAnalysis(...)` sites are `SoilProfile`'s
own `with*` methods; nothing outside `soil-api` constructs them. **Doc fix (same PR):** soil
`CLAUDE.md` says the status band is `OPTIMAL`; the enum constant is `SATISFACTORY` — correct it.

### B. `LabAnalysisInfo` FK + test identifiers

`LabAnalysisInfo` gains `SoilProfileName soilProfileName` (validated `.entityName(soilProfileName,
"soilProfileName")`). `TestSoilIdentifiers` mirrors the graph:

```
SoilProfiles
  Box1          { SoilProfileName name;  LabAnalyses { LabAnalysisId ch2671853_001; } }
  BackyardNorth { SoilProfileName name;  LabAnalyses { LabAnalysisId ch2671853_002; } }
  BackyardCenter{ SoilProfileName name; }
  BackyardSouth { SoilProfileName name; }
  NotFound      { SoilProfileName soilProfile; LabAnalysisId labAnalysis; }
```

### C. Fixtures (real data)

- `SoilProfileInfoTestEntitySource` + `soil/profile/soil-profile-info.json` — 4 profiles
  (`box1`→zone `box-1`; `backyard-*`→zone `backyard`, subzone `backyard-*`; exact zone slugs
  are soft refs, reconciled in Slice 2).
- `LabAnalysisInfoTestEntitySource` + `soil/observation/lab-analysis.json` — the 2 real analyses,
  each with its `soilProfileName` FK and full faithful-chemistry panel from the appendix.
- Each source: a `TestEntitySourceTest` smoke subclass. Note the **≥4 entities** rule — met by
  the 4 profiles; `LabAnalysisInfo` has only 2, so `LabAnalysisInfoTestEntitySource` overrides the
  smoke test or the source carries ≥4 rows. **Resolution:** seed all four profiles' analyses —
  `box1`(-001) and each backyard subzone reusing the `-002` panel with its own `LabAnalysisId`
  — giving 4 `LabAnalysisInfo` rows and satisfying the rule honestly (each subzone *does* have a
  soil test on record; they shared one physical sample).

### D. Persistence (two N=1 repository stacks)

Per the chemistry `Product`/`Element` template:
- `SoilProfileInfoRepository extends EntityRepository<SoilProfileName, SoilProfileInfo>` (no
  domain method) + `SoilProfileInfoEntityRepositoryMock` + `SoilProfileInfoEntityRepositoryTest`
  + mock test.
- `LabAnalysisRepository extends EntityRepository<LabAnalysisId, LabAnalysis>` with
  `List<LabAnalysis> getBySoilProfileName(SoilProfileName)` + mock (validates the arg via
  `observer().arguments("getBySoilProfileName", i -> i.entityName(...)).throwWhenInvalid()`,
  filters `entityStream()`) + contract test (adds the 3 `getBySoilProfileName` cases) + mock test.

### E. Assembly + query + test context

- `SoilProfileInfoQuery extends EntityQuery<SoilProfileName, SoilProfileInfo, SoilProfileInfoCollection>`
  and `LabAnalysisQuery extends EntityQuery<LabAnalysisId, LabAnalysis, LabAnalysisCollection>`
  with `LabAnalysisCollection forSoilProfileName(SoilProfileName)`; thin `*QueryImpl` in
  `soil-core` (extend `AbstractEntityQuery`, implement `findByNameSet`/`forSoilProfileName`).
  Two `BehavioralCollection` finals: `SoilProfileInfoCollection`, `LabAnalysisInfoCollection`.
- `SoilProfileQuery` (public, top-level): `Optional<SoilProfile> getBySoilProfileName(SoilProfileName)`.
  `SoilProfileQueryImpl` delegates to `SoilProfileFactory`.
- `SoilProfileFactory` (package-private, `soil-core`): ctor takes `SoilProfileInfoQuery` +
  `LabAnalysisInfoQuery`; `buildByName(SoilProfileName)` = `infoQuery.getByName(name).map(info ->
  observe(new SoilProfile(info, labAnalysisQuery.forSoilProfileName(name).stream().toList(),
  null, List.of(), List.of(), List.of(), List.of())))`.
- `SoilTestContext` (new `soil-test-context` module) + `SoilTestContextInternal` (`soil-core`
  test) wiring both mocks → both `*QueryImpl` → factory → `SoilProfileQueryImpl`. Module
  scaffolding: new dir + pom, `<module>` entry in `domains/soil/pom.xml`, root
  `dependencyManagement` entry `${project.version}`.

## PR breakdown (dependency order)

1. **Model** — `NutrientPanel` faithful-chemistry + `CationBaseSaturation` + `LabAnalysisInfo`
   `soilProfileName` FK + soil `CLAUDE.md` doc fix. `soil-api` only. Tests: VO invariant tests.
2. **Identifiers + fixtures** — `TestSoilIdentifiers`; both `TestEntitySource`s + JSON + smoke
   tests. Adds `soil-repository-test` deps (framework-test, soil-api, identifiers-test).
3. **Persistence** — both repository interfaces + mocks + behavioral contracts + mock tests.
4. **Assembly + query** — collections, child queries + impls, `SoilProfileFactory`,
   `SoilProfileQuery` + impl, `soil-test-context` module, `SoilTestContextInternal`, query
   contract tests + a factory test asserting `getBySoilProfileName("box1")` yields the profile
   with its `-001` analysis and empty event lists.

## Testing

VO invariant tests via the Observer idiom (`mo.observable(vo,"x")` → `violations()` empty /
`violationNamesRemovingPrefix(mo.observationPoint())`), not `assertThrows`. Repository
behavioral contracts (3 cases/method) + `getBySoilProfileName` cases. `TestEntitySourceTest`
smoke (≥4). Query contracts via `EntityQueryContractTest`. Factory test over
`SoilTestContextInternal`. **Build/verify is the user's `mvn verify` from root — not run in
this session; per-file IDE checks used during implementation.**

## Next program (the pivot) & open modeling questions

After the foundation, the agreed next program is the **CropProfile catalog + interactive
interpretation** — not the zone-wiring spine (parked). Captured here so the foundation's
compatibility is explicit:

- **CropProfile** = a managed, *sourced* optimum-range set keyed by `(crop, source)`, harvested
  from the per-reading optimum snapshots the foundation persists. Applying a different
  CropProfile to a profile's stored measured values yields a **derived** status — the
  interactive "re-color the report for lettuce / for another authority's ranges" experience.
- **CEC-scaled ranges — a learning opportunity.** The real FGL data shows Box 1 and Backyard
  carry *different* optimum ranges for the *same* crop (tomato) because FGL scales targets by
  the soil's CEC (44.9 vs 34.2 meq/100g). The foundation already captures both sides (per-reading
  optimum + panel CEC), so the relationship is recoverable. The CropProfile catalog must model
  this CEC-dependence, and the UI should *teach* it ("same crop, different targets — here's
  why") — a first-class learning surface in the spirit of the Durrell descriptions and glossary.
- **Open: the reading grain.** Whether `NutrientReading` stays a value object (cross-reading
  analytics served by a read-side projection over stored analyses) or is promoted to a persisted
  entity / fact-grain (direct per-reading queries across profiles, crops, time). The foundation
  supports **both** — analyses store readings as value objects today; a projection reads from
  them, or readings can later be promoted. Decide inside the CropProfile program with its own
  design pass, **not** by reworking the foundation again.

## Appendix — Source data (FGL report CH 2671853, sampled 2026-03-03)

Transcribed from `domains/soil/FGLDocCH_2671853.pdf`. Fixture PR transcribes **values** from
here. Units lbs/1000 sqft unless noted. Optimum ranges are the **lab's per-sample** ranges.
The **Status** column is provisional (derived from value-vs-range) — verify each against the
PDF's colored bar (micros especially, where above-optimum is often still "Good"→`SATISFACTORY`).

### Box 1 — `CH 2671853-001` (profile `box1`)

| Reading | Value | Optimum | Status |
|---|---|---|---|
| Nitrate-N | 1.36 | 5.3–7.2 | VERY_LOW |
| Phosphorus P₂O₅ | 39.6 | 10–12 | VERY_HIGH |
| Potassium-K₂O (exch) | 40.5 | 19–120 | SATISFACTORY |
| Potassium-K₂O (sol) | 1.31 | 10–19 | VERY_LOW |
| Calcium (exch) | 616 | 500–660 | SATISFACTORY |
| Calcium (sol) | 6.99 | 16–26 | LOW |
| Magnesium (exch) | 115 | 50–100 | HIGH |
| Magnesium (sol) | 2.29 | 5.6–9.0 | LOW |
| Sodium (exch) | 3.9 | 0–47 | SATISFACTORY |
| Sodium (sol) | 0.95 | < 19 | SATISFACTORY |
| Sulfate | 2.73 | 18–110 | VERY_LOW |
| Zinc | 6.35 | 0.39–4.0 | SATISFACTORY |
| Manganese | 2.17 | 0.77–6.2 | SATISFACTORY |
| Iron | 19.7 | 3.7–9.3 | SATISFACTORY |
| Copper | 0.698 | 0.11–3.8 | SATISFACTORY |
| Boron | 0.0202 | 0.072–0.18 | VERY_LOW |
| Chloride | 0.345 | 1.9–21 | VERY_LOW |

CEC 44.9 meq/100g · pH 7.2 · EC 0.504 dS/m · Limestone 1.7% · physical Saturation 126%.
Base sat: Ca 74.6 · Mg 22.9 · K 2.09 · Na 0.408 · H 1.00 (reported `<1.00`).

### Backyard — `CH 2671853-002` (profiles `backyard-north/center/south`)

| Reading | Value | Optimum | Status |
|---|---|---|---|
| Nitrate-N | 1.74 | 3.0–4.8 | VERY_LOW |
| Phosphorus P₂O₅ | 43.3 | 8.4–10 | VERY_HIGH |
| Potassium-K₂O (exch) | 40.6 | 15–89 | SATISFACTORY |
| Potassium-K₂O (sol) | 1.59 | 6.5–15 | VERY_LOW |
| Calcium (exch) | 486 | 380–500 | SATISFACTORY |
| Calcium (sol) | 8.35 | 9.2–20 | LOW |
| Magnesium (exch) | 73.9 | 38–76 | SATISFACTORY |
| Magnesium (sol) | 2.44 | 2.8–6.2 | LOW |
| Sodium (exch) | 4.9 | 0–36 | SATISFACTORY |
| Sodium (sol) | 1.70 | < 26 | SATISFACTORY |
| Sulfate | 3.85 | 11–99 | VERY_LOW |
| Zinc | 4.86 | 0.23–3.9 | SATISFACTORY |
| Manganese | 1.25 | 0.46–5.9 | SATISFACTORY |
| Iron | 14.6 | 2.1–7.8 | SATISFACTORY |
| Copper | 0.854 | 0.060–3.8 | SATISFACTORY |
| Boron | 0.0222 | 0.048–0.16 | VERY_LOW |
| Chloride | 0.889 | 1.1–20 | VERY_LOW |

CEC 34.2 meq/100g · pH 7.2 · EC 0.662 dS/m · Limestone 2.9% · physical Saturation 75.3%.
Base sat: Ca 77.2 · Mg 19.4 · K 2.75 · Na 0.681 · H 1.00 (reported `<1.00`).

### Excluded from the model (deferred)

SAR, Lime Requirement, Gypsum Requirement, soil texture class, Fertilization Recommendations.
