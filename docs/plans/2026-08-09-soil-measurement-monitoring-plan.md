# Soil Measurement Monitoring — Slice 1 (Measurement Grain) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans or
> subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Rebuild the soil chemistry model to the fact-grain shape — nutrients and physical
characteristics as separate entity families, `LabAnalysisInfo` as a thin header, and the panel
assembled as a read model — seeded with the real March FGL data.

**Supersedes:** `docs/plans/2026-08-09-soil-foundation-plan.md` (embedded-panel model). Design:
`docs/plans/2026-08-09-soil-measurement-monitoring-design.md`.

**Architecture:** `NutrientReading` (Entity, one per nutrient per analysis, single repository) and
`SoilPhysicalCharacteristics` (Entity, one per analysis, typed) both reference a `LabAnalysisInfo`
header by `labAnalysisId`. `NutrientPanelFactory` assembles the `NutrientPanel` **ReadModel** from
readings bucketed by category; `LabAnalysisView` composes header + panel + characteristics.

## Progress (2026-08-09)

**DONE, IDE-clean:** Task 1 (identifiers + `Nutrients` catalog); Task 2 (`NutrientReading` entity +
single repository + 68-row fixtures + contract/mock/smoke tests); Task 3 (`SoilPhysicalCharacteristics`
entity + repository + 4-row fixtures + tests); Task 4 (`LabAnalysisInfo` slimmed to a header, `SoilProfile`
BER delegation removed, fixtures + contract test reshaped); Task 5 **model reclassification**
(`Primary/Secondary/Micro`/`NutrientPanel` → `ReadModel`, BER moved to `NutrientPanel`, limestone to
`SoilPhysicalCharacteristics`). The whole soil tree (api/core/repository-test/identifiers) verifies
error-free in the IDE.

**REMAINING:** Task 5 assembly (`NutrientPanelFactory`, `LabAnalysisView` + its factory) and Task 6
(queries + collections for the two new entities, test-context wiring, `SoilProfile` re-evaluation).
Note: FK constraints on the new sources were **omitted** (deferred to RDBMS) so contract-test inserts
with random `labAnalysisId`s don't trip them — unique constraints kept.

## Global constraints

- Framework types: `EntityRepository` / `EntityQuery` / `EntityRepositoryTest` / `TestEntitySource`
  / `AbstractEntityQuery` / `AbstractTestEntityRepository`. No composite natural key → surrogate
  `EntityId` + `@UniqueValue`/unique-constraint on the logical key.
- **ADR-013:** a `ValueObject` may not contain an `Entity`. Since `NutrientReading` is now an
  Entity, `PrimaryNutrients` / `SecondaryNutrients` / `MicroNutrients` / `NutrientPanel` become
  `ReadModel`s.
- `TestEntitySource` smoke test requires ≥4 rows per source.
- Invariant tests use the Observer idiom, not `assertThrows`. Real FGL fixture data.
- Build/verify is the user's `mvn verify`; this session uses IDE per-file checks. No commit until asked.

## Superseded files to remove/reshape

- **Reshape:** `NutrientReading` (VO→Entity), `NutrientPanel`+`Primary/Secondary/Micro` (VO→ReadModel),
  `LabAnalysisInfo` (drop `nutrients`, drop panel-dependent helpers), `lab-analysis.json`,
  `LabAnalysisInfoTestEntitySource`/its identifiers, `LabAnalysisInfoEntityRepositoryTest` samplePanel.
- **Re-evaluate:** `SoilProfileFactory` / `SoilProfileQuery` / `SoilProfile` aggregate — the profile
  view likely composes from measurements now; decide in Task 6 (design says "iterate in code").

---

### Task 1: Identifiers + category

**Files:**
- Create `domains/identifiers/src/main/java/com/naturalist/soil/observation/NutrientName.java`
  (`EntityName`, `@JsonCreator of(String)`, maxLength ~48) — slugs `nitrate-n`, `phosphorus-p2o5`,
  `potassium-exchangeable`, `potassium-soluble`, `calcium-exchangeable`, `calcium-soluble`,
  `magnesium-exchangeable`, `magnesium-soluble`, `sodium-exchangeable`, `sodium-soluble`, `sulfate`,
  `zinc`, `manganese`, `iron`, `copper`, `boron`, `chloride`.
- Create `.../observation/NutrientReadingId.java` (`EntityId`, `of(UUID)` + `create()`).
- Create `.../observation/SoilPhysicalCharacteristicsId.java` (`EntityId`).
- Create `soil-api/.../observation/NutrientCategory.java` enum `{ PRIMARY, SECONDARY, MICRO }`.
- Create `soil-api/.../observation/Nutrients.java` — a static domain catalog mapping
  `NutrientName → NutrientCategory` (the 17 names, each with a `NutrientName` constant + category),
  used by the panel factory to bucket. Single source of truth for "which nutrients exist and their
  category."

- [x] Create the three identifier types + category enum + `Nutrients` catalog. IDE-check. **DONE — IDE-clean.**

### Task 2: NutrientReading entity + repository + fixtures

**Files:**
- Reshape `soil-api/.../observation/NutrientReading.java` → `record NutrientReading(NutrientReadingId
  id, NutrientName nutrientName, LabAnalysisId labAnalysisId, BigDecimal value, OptimumRange optimum,
  NutrientStatus status) implements Entity<NutrientReadingId>`. `invariants()`: entityId(id),
  entityName(nutrientName), entityId(labAnalysisId), notNull(value), valueObject(optimum),
  notNull(status). Unique constraint declared on `(nutrientName, labAnalysisId)`.
- Create `soil-api/.../observation/NutrientReadingRepository.java` (pkg-private) extends
  `EntityRepository<NutrientReadingId, NutrientReading>` + `List<NutrientReading>
  getByLabAnalysisId(LabAnalysisId)` + `List<NutrientReading> getByNutrientName(NutrientName)`.
- Create mock `NutrientReadingEntityRepositoryMock` (validates both query args, filters stream);
  declare the `(nutrientName, labAnalysisId)` unique constraint in the TestEntitySource.
- Create `NutrientReadingTestEntitySource` + `soil/observation/nutrient-reading.json` — 68 rows
  (4 analyses × 17 nutrients), values/optimum/status from the design appendix (box1 = -001 ranges;
  the three backyard subzones = -002 ranges). Generate rather than hand-type to avoid transcription
  error (script in scratchpad, emit static JSON).
- Create contract test `NutrientReadingEntityRepositoryTest` (hooks + the two `getBy*` cases) + mock
  test. Add representative `NutrientReadingId` + `NutrientName` constants to `TestSoilIdentifiers`
  (≥2 known readings + a `NotFound`).

- [ ] Entity; repository interface; mock; source + JSON (script-generated); contract + mock tests;
      identifiers. IDE-check.

### Task 3: SoilPhysicalCharacteristics entity + repository + fixtures

**Files:**
- Create `soil-api/.../observation/SoilPhysicalCharacteristics.java` → `record
  SoilPhysicalCharacteristics(SoilPhysicalCharacteristicsId id, LabAnalysisId labAnalysisId,
  SoilPH pH, ElectricalConductivity ec, CecMeqPer100g cec, LimestonePct limestone,
  SaturationPct saturation, CationBaseSaturation baseSaturation) implements
  Entity<SoilPhysicalCharacteristicsId>`. Unique on `labAnalysisId`.
- Repository (`getByLabAnalysisId`) + mock + `SoilPhysicalCharacteristicsTestEntitySource` +
  `soil/observation/soil-physical-characteristics.json` (4 rows, real values) + contract + mock test.

- [ ] Entity; repository stack; source + JSON (4 rows); tests; identifiers. IDE-check.

### Task 4: LabAnalysis header

**Files:**
- Reshape `soil-api/.../observation/LabAnalysis.java` → `record LabAnalysis(LabAnalysisId id,
  SoilProfileName soilProfileName, Crop crop, LocalDate sampleDate, String labId, String labSampleId,
  @Nullable String notes) implements Entity<LabAnalysisId>` — drop `nutrients`. Move
  `indicatesBerRisk()` / `hasThiobacillusAmenableLimestone()` OFF (to `LabAnalysisView` / factory in
  Task 5).
- Update `LabAnalysisInfoTestEntitySource` JSON `lab-analysis.json` → 4 header rows (no `nutrients`).
- Update `LabAnalysisInfoEntityRepositoryTest` `newEntity`/`modifiedEntity` (drop the panel; header only).

- [ ] Slim the header; update fixtures + contract test. IDE-check (expect the BER helper move to
      surface consumers; fix them in Task 5).

### Task 5: Assembled read models

**Files:**
- Reshape `PrimaryNutrients` / `SecondaryNutrients` / `MicroNutrients` / `NutrientPanel` →
  `implements ReadModel` (they now hold `NutrientReading` entities). `NutrientPanel` gains the
  BER / thiobacillus query methods (moved from `LabAnalysisInfo`).
- Create `soil-core/.../observation/NutrientPanelFactory.java` — `NutrientPanel build(List<NutrientReading>)`:
  bucket by `Nutrients.categoryOf(name)` into the three group read models. Package-private.
- Create `soil-api/.../observation/LabAnalysisView.java` (`ReadModel`): header + `NutrientPanel` +
  `SoilPhysicalCharacteristics`. Assembled by a `LabAnalysisViewFactory` in soil-core from the three
  repositories/queries.
- Tests: `NutrientPanelFactoryTest` (17 readings → correct buckets; BER logic), `LabAnalysisViewFactoryTest`.

- [ ] ReadModel reclassification; the two factories; move + retest the BER/limestone logic. IDE-check.

### Task 6: Queries, test context, profile re-evaluation

**Files:**
- `NutrientReadingQuery` / `SoilPhysicalCharacteristicsQuery` / `LabAnalysisInfoQuery` (header) +
  `LabAnalysisView` query returning `Optional<LabAnalysisView>` → thin impls in soil-core +
  collections. `SoilsTestContextInternal` wires the new repositories/factories.
- **Re-evaluate `SoilProfile`:** with the panel assembled and analyses queried directly, decide
  whether `SoilProfileQuery` returns an aggregate or the profile view composes
  `SoilProfileInfo` + its `LabAnalysisView`s directly. Iterate with the code in front of us; adjust
  or retire `SoilProfileFactory` accordingly.

- [ ] Queries + impls + collections; test context; profile decision. IDE-check.

## Self-review notes

- Coverage: Tasks 1–6 cover identifiers, the two entity families, the header, the assembled views,
  and the read surface — matching the design's "Core model" and "Slice 1" section.
- Risk: built without `mvn`; IDE checks only. The 68-row nutrient JSON is the highest transcription
  risk — generate it from the appendix values with a scratchpad script, don't hand-type.
- The `CropProfile` catalog, monitoring series, and interactive/teaching surfaces are Slices 2–4
  (separate plans), not here.
