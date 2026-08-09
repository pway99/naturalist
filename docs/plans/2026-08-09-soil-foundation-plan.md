# Soil Foundation (Slice 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans or
> subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Bring the soil domain from api-only skeleton to a persisted, assembled, queryable
`SoilProfile` aggregate seeded with the real March 2026 FGL data.

**Architecture:** `SoilProfileInfo` (NamedEntity) and `LabAnalysisInfo` (Entity, gaining a
`soilProfileName` FK) are persisted through `EntityRepository` mocks backed by
`TestEntitySource` JSON. `SoilProfileFactory` in `soil-core` assembles the `SoilProfile`
aggregate from `SoilProfileInfoQuery` + `LabAnalysisQuery.forSoilProfileName`, with empty event
lists (events deferred). `SoilProfileQuery.getBySoilProfileName → Optional<SoilProfile>`.

**Tech Stack:** Java 25 records, `com.naturalist.data` framework, JUnit 5 + AssertJ, Jackson
2.19 record-native, Maven multi-module.

## Global Constraints

- Framework types are `TestEntitySource` / `EntityRepository` / `EntityRepositoryTest` /
  `EntityQuery` / `AbstractEntityQuery` / `EntityQueryContractTest` (NOT the `Named*` names in
  CLAUDE.md prose).
- Repository interfaces package-private in `soil-api`; queries public; factory package-private
  in `soil-core`, never in `soil-api`.
- `new Foo(...)` only inside the type's own class; elsewhere use static factories.
- `@JsonCreator` on `EntityName`/`NamedValue` `of(...)` only — never on records.
- Invariant tests use the Observer idiom, not `assertThrows`.
- Fixtures use real FGL values (appendix of the design doc). `TestEntitySourceTest` requires ≥4 rows.
- **Build/verify:** user runs `mvn verify` from repo root. This session cannot run mvn; use
  IDE per-file problem checks. Do not claim "passing" — claim "written, IDE-clean, awaiting `mvn verify`".
- No commits unless the user says "commit".

---

### Task 1: Model — NutrientPanel faithful chemistry + CationBaseSaturation + LabAnalysis FK

**Files:**
- Create: `domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/CationBaseSaturation.java`
- Modify: `.../observation/NutrientPanel.java` (8 new readings + `cationBaseSaturation`)
- Modify: `.../observation/LabAnalysis.java` (add `SoilProfileName soilProfileName`)
- Modify: `domains/soil/CLAUDE.md` (`OPTIMAL` → `SATISFACTORY`)
- Test: `.../observation/CationBaseSaturationTest.java`, `.../observation/NutrientPanelTest.java`

**Interfaces produced:**
- `CationBaseSaturation(BigDecimal calciumPct, magnesiumPct, potassiumPct, sodiumPct, hydrogenPct)`
- `NutrientPanel(... existing ..., magnesiumExch, sodiumExch, sodiumSoluble, zinc, manganese,
  iron, copper, chloride, cationBaseSaturation)` — new fields are `NutrientReading` except the last.
- `LabAnalysis(id, soilProfileName, sampleDate, labId, labSampleId, notes, nutrients)`

- [ ] Write `CationBaseSaturationTest` (valid → no violations; all-null → 5 field violations;
      out-of-range pct → violation).
- [ ] Implement `CationBaseSaturation` (`inRange(v, BigDecimal.ZERO, BigDecimal.valueOf(100))`).
- [ ] Extend `NutrientPanel` + invariants; extend `NutrientPanelTest`.
- [ ] Add `soilProfileName` to `LabAnalysisInfo` + invariant `.entityName(...)`.
- [ ] Fix soil `CLAUDE.md` status band wording.
- [ ] IDE-check all touched files; (user) `mvn verify`.

### Task 2: Identifiers + fixtures

**Files:**
- Create: `domains/identifiers-test/src/main/java/com/naturalist/soil/TestSoilIdentifiers.java`
- Create: `domains/soil/soil-repository-test/src/main/java/com/naturalist/soil/SoilProfileInfoTestEntitySource.java`
- Create: `.../soil-repository-test/src/main/java/com/naturalist/soil/observation/LabAnalysisTestEntitySource.java`
- Create: `.../soil-repository-test/src/main/resources/soil/profile/soil-profile-info.json` (4 profiles)
- Create: `.../soil-repository-test/src/main/resources/soil/observation/lab-analysis.json` (4 analyses)
- Create: smoke tests `SoilProfileInfoTestEntitySourceTest`, `LabAnalysisInfoTestEntitySourceTest`
- Modify: `domains/soil/soil-repository-test/pom.xml` (deps: framework-test, soil-api, identifiers-test)

- [ ] Add repository-test deps.
- [ ] `TestSoilIdentifiers` per §B of the design.
- [ ] Both `TestEntitySource` subclasses (single `(NaturalistDatabase)` ctor + `loadFile`).
- [ ] Both JSON catalogs from the appendix (no `id` for the profile; `LabAnalysisId` UUIDv7 for analyses).
- [ ] Both smoke tests extend `TestEntitySourceTest<…>` (≥4 rows each).
- [ ] IDE-check; (user) `mvn verify`.

### Task 3: Persistence — two N=1 repository stacks

**Files (soil-api):**
- Create: `.../soil/SoilProfileInfoRepository.java` (pkg-private `interface extends EntityRepository<SoilProfileName, SoilProfileInfo>`)
- Create: `.../soil/observation/LabAnalysisRepository.java` (+ `List<LabAnalysis> getBySoilProfileName(SoilProfileName)`)

**Files (soil-repository-test):**
- Create mocks `SoilProfileInfoEntityRepositoryMock`, `LabAnalysisInfoEntityRepositoryMock`
  (`@DomainService`, extend `AbstractTestEntityRepository<…, TestEntitySource>`).
- Create contracts `SoilProfileInfoEntityRepositoryTest`, `LabAnalysisInfoEntityRepositoryTest`
  (`extends EntityRepositoryTest<…>`, supply hooks, `LabAnalysisInfo` adds `getBySoilProfileName` cases).
- Create mock tests `*MockTest` (`implements` the contract, wire `repository()` from `db`).

- [ ] Interfaces; mocks; contracts (`newEntity`/`ghostEntity`/`modifiedEntity` via `RandomValue`);
      mock tests. `LabAnalysisInfo` mock validates `getBySoilProfileName` arg + filters stream.
- [ ] IDE-check; (user) `mvn verify`.

### Task 4: Assembly + query + test context

**Files (soil-api):**
- Create: `.../soil/SoilProfileInfoQuery.java`, `.../soil/SoilProfileInfoCollection.java`
- Create: `.../soil/observation/LabAnalysisQuery.java` (+ `forSoilProfileName`), `.../observation/LabAnalysisCollection.java`
- Create: `.../soil/SoilProfileQuery.java` (`Optional<SoilProfile> getBySoilProfileName(SoilProfileName)`)

**Files (soil-core, new src tree):**
- Create: `SoilProfileInfoQueryImpl`, `LabAnalysisInfoQueryImpl` (extend `AbstractEntityQuery`),
  `SoilProfileFactory`, `SoilProfileQueryImpl`.
- Test: `SoilProfileInfoQueryImplTest`, `LabAnalysisInfoQueryImplTest`, `SoilProfileFactoryTest`,
  `SoilsTestContextInternal`.

**Files (new `soil-test-context` module):**
- Create module dir + pom; `<module>soil-test-context</module>` in `domains/soil/pom.xml`;
  root `dependencyManagement` entry (`${project.version}`, SOIL section).
- Create: `SoilTestContext` (public, wires mocks → queries → factory → `SoilProfileQueryImpl`).

- [ ] Collections; child queries + impls; factory; `SoilProfileQuery` + impl.
- [ ] `soil-test-context` module scaffold + `SoilTestContext`; `SoilsTestContextInternal` in core test.
- [ ] Query contract tests + `SoilProfileFactoryTest` (`getBySoilProfileName("box1")` → profile
      with `-001` analysis, empty events).
- [ ] IDE-check; (user) `mvn verify`.

## Self-review notes

- Spec coverage: Tasks 1–4 cover model, fixtures, persistence, assembly/query respectively.
- Risk: built without `mvn`; IDE checks only. Highest-uncertainty pieces: exact `AbstractEntityQuery`
  generic wiring for the factory-backed `SoilProfileQuery` (it is NOT an `EntityQuery` — it is a
  bespoke interface like `TaxonViewQuery`, so `SoilProfileQueryImpl` does NOT extend
  `AbstractEntityQuery`; it holds the factory directly, mirroring `TaxonViewQueryImpl`).
- The `soil-repository-rdms` module stays empty this slice.
