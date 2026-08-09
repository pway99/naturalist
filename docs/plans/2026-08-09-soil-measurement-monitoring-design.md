# Soil Measurement Monitoring & CropProfile Catalog — Design

**Date:** 2026-08-09
**Status:** Design — brainstormed with the user; supersedes the embedded-panel foundation model.
**Program:** the pivot from soil→zone navigation. This is the analytics program the soil
foundation now feeds. The soil→zone navigation spine is parked.

## Problem & driving use case

Patrick is rehabilitating the Oak Vista soil over multiple years — regular gypsum additions to
lift soluble calcium, displace K/Na from the coir, and precipitate excess phosphorus — and needs
to **monitor a chosen set of measurements across periodic lab tests** to see the trajectory
work: soluble Ca climbing toward optimum, sulfate rising, EC staying under 2.0, base saturation
shifting. This is the report's own *Annual Monitoring Protocol* and *Multi-Year Rehabilitation
Trajectory*, made live.

Three capabilities fall out:

1. **Monitoring** — pick a set of measurements (soluble Ca, sulfate, EC, pH, …) and see each as a
   time series across analyses, against its target and action trigger.
2. **Interactive re-interpretation** — the measured values are fixed facts; the *optimum ranges*
   depend on crop and soil buffering. Apply a different **CropProfile** (tomato → lettuce, or FGL
   → another authority) and watch the statuses re-color. A learning tool.
3. **Teaching** — surface *why*, e.g. "Box 1 and Backyard are both tomato but have different
   targets because FGL scales them by CEC (44.9 vs 34.2)."

## Core model — measurements as facts, two entity families

The measured value is the definitive, crop-independent fact and the queryable grain. Nutrients
and physical characteristics are **separate entity families** so each keeps its natural typing.

### NutrientReading (fine grain)

A per-nutrient fact — one row per nutrient per analysis; the grain that makes "monitor a select
set of nutrients over time" a direct query.

- `Entity<NutrientReadingId>` (surrogate UUIDv7 — this framework has **no composite natural key**,
  so the logical key `(nutrientName, labAnalysisId)` is a declared **unique constraint**).
- Components: `nutrientName` (new `NutrientName` `EntityName` — `nitrate-n`,
  `phosphorus-p2o5`, `potassium-exchangeable`, `potassium-soluble`, `calcium-exchangeable`,
  `calcium-soluble`, `magnesium-exchangeable`, `magnesium-soluble`, `sodium-exchangeable`,
  `sodium-soluble`, `sulfate`, `zinc`, `manganese`, `iron`, `copper`, `boron`, `chloride`),
  `labAnalysisId` (FK), `value` (BigDecimal), `unit` (`MeasurementUnit` — FGL nutrients are
  `LBS_PER_1000_SQFT`). **A pure measured fact — no optimum range, no status.** Optimum/status are
  interpretation, derived by applying a `CropProfile`, never stored on the reading. (Physical
  characteristics need no `unit` — their values are typed, e.g. `ElectricalConductivity` = dS/m.)
- A slug identifier (not an enum) keeps the panel open to new nutrients / other labs and is the
  join key for a `CropProfile`.
- **One `NutrientReadingRepository`** holding all readings across all analyses;
  `getByLabAnalysisId(id)` (assemble a panel) and `getByNutrientName(name)` (time series).

### SoilPhysicalCharacteristics (coarse grain, richly typed)

One per analysis — the differently-typed, differently-united properties that don't slice like
nutrients and must keep their validation.

- `Entity<SoilPhysicalCharacteristicsId>` keyed 1:1 to a `LabAnalysisInfo` (surrogate id + unique
  `labAnalysisId`).
- Components keep today's typed value objects: `SoilPH`, `ElectricalConductivity`,
  `CecMeqPer100g`, `LimestonePct`, `SaturationPct`, `CationBaseSaturation`. **No loss of typing.**
- Own repository; `getByLabAnalysisId(id)` and (for monitoring) a way to walk one property across
  analyses.
- **Grain decision (with a flagged tension).** Physical characteristics are a **separate entity
  family** from nutrients (own repository — they *are* fundamentally different), and stay **typed**
  (fixed typed fields) rather than an open `name+value` grain. This trades the "never locked to a
  finite set" goal *for characteristics only* — accepted because the set is small and standard
  (~6), the typed validation (pH 0–14, units) has real value, and a novel characteristic is a cheap
  typed-field add. Nutrients (where lab-variability is real) keep the open grain. Revisit in code if
  a lab surfaces genuinely open-ended characteristics.

### LabAnalysis (header) + assembled views

- `LabAnalysisInfo` becomes a thin header: `(id, soilProfileName, crop, sampleDate, labId,
  labSampleId, notes)` — no embedded panel.
- The `NutrientPanel` and its `PrimaryNutrients`/`SecondaryNutrients`/`MicroNutrients` groups
  become **assembled `ReadModel` views**: a `NutrientPanelFactory` loads readings by
  `labAnalysisId` and buckets them by category (`NutrientName → NutrientCategory`, domain
  knowledge). A `LabAnalysisView` composes the header + assembled panel + physical characteristics.

## CropProfile catalog — optimum ranges as managed reference data

Optimum ranges are **not** stored on the measured facts. They are managed, sourced reference data.

- **`CropProfile`** — keyed by **`(source, crop)`** (the comparison axis: `fgl-tomato` vs.
  `extension-office-tomato`). A `NamedEntity` with a slug like `fgl-tomato`. Maps
  `nutrientName → OptimumRange`, plus characteristic targets (pH 6.5–7.5, EC < 2.0, limestone < 0.50).
- **CEC as a field, scaling deferred (decided).** The real FGL data proves ranges scale with soil
  buffering (CEC), not crop alone — but keying by CEC would break the clean `(source, crop)`
  comparison axis. So the profile carries a descriptive **`referenceCec`** field (the CEC its ranges
  were calibrated at / harvested from), which powers the teaching surface ("these targets assume
  CEC ≈ 40; your soil is 34"). The actual CEC-**scaling function** (adjusting ranges to a soil's
  CEC) is a deferred refinement — for now a profile's ranges apply as-is.
- **Harvested** from reports — the FGL report's printed ranges become the `fgl-tomato@CEC` profile.
  Other sources (university extension, research) are added for comparison.
- **Applied** to a profile's stored measured values → a **derived** status. The interactive
  experience swaps the applied `CropProfile` and re-derives.

### Status: a dated assessment, not a stored field (decided)

Status is **not** on the reading. A measurement has no "status" until a crop lens is chosen, so a
status is a **dated assessment** = apply a `CropProfile` to a profile's measurements *as of a date*.
The FGL report becomes the **pre-amendment assessment** (March, before gypsum); later tests give
later assessments, and the sequence is the rehabilitation trajectory. These assessments live on the
**crop-planting timeline** (below), interleaved with amendment events — "the status reviewed in
context of amendments." Two statuses coexist there, both derived: the **as-reported** FGL status
(apply the FGL `CropProfile`) and any **re-interpreted** status (apply another crop/source profile).
Whether the `CropProfile` encodes FGL's full 5-band breakpoints exactly, or the as-reported status
is lightly captured, is settled in the CropProfile/crop effort.

### Crop planting — a separate effort (basic concept only)

"Crop" is two things. The soil layers here use only the **crop *type*** — a `CropName` soft-reference
(`LabAnalysis.crop`) that keys the `CropProfile`. The **crop *planting*** — a season's planting of a
variety across one or more soil profiles, owning its **amendment timeline**, its **dated nutrient
assessments**, and its **outcomes** (yield — 96 quarts canned in 2026 — and per-profile fruit
observations like "small for their variety") — is a rich aggregate that intersects soil *and* the
plants domain. It gets its **own brainstorm/effort**; captured here only enough to guide these soil
slices, not designed. The FGL as-reported status is fine to capture there.

## Monitoring series

- A `MeasurementSeries` read model: given a set of measurement names + a soil profile, return each
  as a time-ordered series of `(sampleDate, value, appliedOptimum, derivedStatus)` across the
  profile's analyses, with the target and **action trigger** (the report's Annual Monitoring
  Protocol: "soluble Ca below 10 → increase CalMag foliar", "EC above 1.5 → reduce gypsum").
- Draws from **both** entity families (nutrient readings + physical characteristics) — the series
  view is where the two families unify for display.
- The gypsum rehabilitation trajectory (Correction → Transition → Stabilization → Maintenance) is
  the narrative layer over these series.

## Relationship to the built foundation

The Slice-1 foundation (`SoilProfileInfo` + `LabAnalysisInfo` with an **embedded** value-object
`NutrientPanel`, real FGL fixtures, repositories, `SoilProfileFactory`) was built and is IDE-clean.
This design **supersedes the embedded-panel storage**:

- **Carries over:** `SoilProfileInfo` + its repository/query; `LabAnalysisInfo` (slims to a header);
  `Crop`, `OptimumRange`, `NutrientStatus`; the real FGL data (re-shaped from nested panels into a
  flat reading list + a physical-characteristics row per analysis); the `Primary/Secondary/Micro`
  and `CationBaseSaturation` value objects (repurposed as assembly targets / typed physical value).
- **Superseded:** `NutrientPanel` as a stored component of `LabAnalysisInfo` (becomes an assembled
  view); the `lab-analysis.json` panel shape (becomes reading rows + physical rows).
- **Net:** most types survive; what changes is *where readings live* (own repository) and that the
  panel is assembled, not stored. Treat the embedded-panel version as scaffold this replaces.

## Proposed slices

1. **Measurement grain** — `NutrientName` identifier; `NutrientReading` entity + single repository
   (+ `getByNutrientName`, `getByLabAnalysisId`); `SoilPhysicalCharacteristics` entity + repository;
   `LabAnalysisInfo` slimmed to a header; fixtures re-shaped to reading rows + physical rows (real FGL
   data). Panel assembly (`NutrientPanelFactory`, `LabAnalysisView`).
2. **CropProfile catalog** — `CropProfile` entity + repository + fixtures (harvest the FGL tomato
   ranges, CEC-aware); apply-to-values → derived status; the captured vs derived status model.
3. **Monitoring & trajectory** — `MeasurementSeries` read model across analyses; targets + action
   triggers; the gypsum rehabilitation trajectory narrative.
4. **Teaching / interactive surface** — console: monitor a chosen set, swap the applied
   `CropProfile`, and the CEC-scaling explanation.

## Decisions (open questions resolved 2026-08-09)

- **Status — keep both.** The FGL-reported status is an expert-derived value with long-term worth;
  persist it as the captured `as-reported` status. Also support a **derived** status by applying a
  `CropProfile`. Two clearly-labeled statuses.
- **CropProfile key — `(source, crop)`** (clean comparison axis), with a descriptive `referenceCec`
  field; the CEC-scaling function is deferred.
- **Physical characteristics — separate *typed* entity family.** Nutrients keep the open name-keyed
  grain; characteristics stay typed (see the grain note above); the two are distinct families with
  distinct repositories.
- **`SoilProfile` aggregate — iterate in code.** Build the measurement grain first, then decide
  whether the profile view composes directly from measurements + characteristics or keeps an
  assembled aggregate. Revisit with real code in front of us.

## Still to settle during Slice 1

- **Physical-characteristic targets** — live in the `CropProfile` alongside nutrient ranges (pH,
  EC, limestone are broadly crop-stable), or a separate physical-target set. Lean: put stable
  targets in the profile too, since re-interpretation should re-color pH/EC as well.
- Whether characteristics need per-property optimum + captured status (like readings) or just the
  typed value + a target looked up from the profile.
