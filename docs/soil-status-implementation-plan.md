# Soil Status — Implementation Plan

**Rationale reference:** `soil-status-placement-brief.md` (arguments, rejected
alternatives). This document sequences the work; it does not re-argue it.
**Context reference:** `docs/briefings/soil-domain.md` (2026-08-10).
**Hard deadline:** August FGL report, lettuce panel, samples collected week of
2026-08-17.

Each phase below is sized for one Claude Code session. Hand the session this
plan's phase section, `soil-domain.md`, and the named brief section — not the
whole brief.

---

## The model today

What the phases below are editing. Solid diamonds are composition (assembled on
read); dashed arrows are the persisted foreign keys the assembly joins on.
`*Info` = persisted fact; bare noun = read model composed by
`SoilProfileFactory` and never stored.

```mermaid
classDiagram
    direction TB

    class SoilProfile {
        <<ReadModel>>
        SoilProfileInfo info
        List~LabAnalysis~ labAnalyses
        +soilProfileName() SoilProfileName
        +latestLabAnalysis() Optional~LabAnalysis~
    }

    class SoilProfileInfo {
        <<NamedEntity — aggregate root>>
        SoilProfileName name
        ZoneName zoneName
        nullable SubZoneName subZoneName
    }

    class LabAnalysis {
        <<ReadModel>>
        LabAnalysisInfo info
        NutrientPanel nutrients
        Optional~SoilPhysicalCharacteristics~ physicalCharacteristics
        ReportedOptimumCollection reportedOptima
        ReportedRecommendationCollection reportedRecommendations
    }

    class LabAnalysisInfo {
        <<Entity — LabAnalysisId>>
        LabAnalysisId id
        SoilProfileName soilProfileName
        CropName crop
        LocalDate sampleDate
        String labId
        String labSampleId
        nullable DepthInches sampleDepth
        nullable SamplingProtocol samplingProtocol
        nullable String notes
    }

    class ReportedOptimum {
        <<Entity — ReportedOptimumId>>
        ReportedOptimumId id
        NutrientName nutrientName
        LabAnalysisId labAnalysisId
        OptimumRange range
        MeasurementUnit unit
    }

    class OptimumRange {
        <<sealed ValueObject>>
        Closed min max
        UpperBounded max
        LowerBounded min
        NotApplicable
    }

    class ReportedRecommendation {
        <<Entity — ReportedRecommendationId>>
        ReportedRecommendationId id
        RecommendedInputName inputName
        LabAnalysisId labAnalysisId
        RecommendedAmount amount
        MeasurementUnit unit
        nullable ApplicationRoute route
    }

    class RecommendedAmount {
        <<sealed ValueObject>>
        Quantity value
        None
        BelowDetectionLimit limit
    }

    class NutrientPanel {
        <<ReadModel>>
        PrimaryNutrients primary
        SecondaryNutrients secondary
        MicroNutrients micro
    }

    class PrimaryNutrients {
        <<ReadModel>>
        Optional~NutrientReading~ nitrateN
        Optional~NutrientReading~ phosphorusP2O5
        Optional~NutrientReading~ potassiumExch
        Optional~NutrientReading~ potassiumSoluble
    }

    class SamplingProtocol {
        <<ValueObject>>
        int subsampleCount
        SamplingTool tool
        CompositingMethod compositingMethod
    }

    class SecondaryNutrients {
        <<ReadModel>>
        all slots Optional
        calciumExch / calciumSoluble
        magnesiumExch / magnesiumSoluble
        sodiumExch / sodiumSoluble
        sulfate
    }

    class MicroNutrients {
        <<ReadModel>>
        all slots Optional
        zinc / manganese / iron
        copper / boron / chloride
    }

    class NutrientReading {
        <<Entity — NutrientReadingId>>
        NutrientReadingId id
        NutrientName nutrientName
        LabAnalysisId labAnalysisId
        BigDecimal value
        MeasurementUnit unit
    }

    class SoilPhysicalCharacteristics {
        <<Entity — SoilPhysicalCharacteristicsId>>
        SoilPhysicalCharacteristicsId id
        LabAnalysisId labAnalysisId
        CecMeqPer100g cecMeqPer100g
        SoilPH pH
        ElectricalConductivity ecDsPerMeter
        SodiumAdsorptionRatio sar
        LimestonePct limestonePct
        SaturationPct saturationPct
        CationBaseSaturation cationBaseSaturation
    }

    class CationBaseSaturation {
        <<ValueObject>>
        BigDecimal calciumPct
        BigDecimal magnesiumPct
        BigDecimal potassiumPct
        BigDecimal sodiumPct
        BigDecimal hydrogenPct
        boolean hydrogenBelowDetectionLimit
    }

    SoilProfile *-- "1" SoilProfileInfo
    SoilProfile *-- "0..*" LabAnalysis : oldest first
    LabAnalysis *-- "1" LabAnalysisInfo
    LabAnalysis *-- "1" NutrientPanel
    LabAnalysis *-- "0..1" SoilPhysicalCharacteristics
    LabAnalysis *-- "0..*" ReportedOptimum : sibling of the panel
    ReportedOptimum *-- "1" OptimumRange
    ReportedOptimum ..> LabAnalysisInfo : labAnalysisId + nutrientName unique
    LabAnalysis *-- "0..*" ReportedRecommendation : sibling of the panel
    ReportedRecommendation *-- "1" RecommendedAmount
    ReportedRecommendation ..> LabAnalysisInfo : labAnalysisId + inputName unique
    LabAnalysisInfo *-- "0..1" SamplingProtocol
    NutrientPanel *-- "1" PrimaryNutrients
    NutrientPanel *-- "1" SecondaryNutrients
    NutrientPanel *-- "1" MicroNutrients
    PrimaryNutrients *-- "4" NutrientReading
    SecondaryNutrients *-- "7" NutrientReading
    MicroNutrients *-- "6" NutrientReading
    SoilPhysicalCharacteristics *-- "1" CationBaseSaturation

    LabAnalysisInfo ..> SoilProfileInfo : soilProfileName
    NutrientReading ..> LabAnalysisInfo : labAnalysisId + nutrientName unique
    SoilPhysicalCharacteristics ..> LabAnalysisInfo : labAnalysisId unique
```

Soft cross-domain references, carried by name and never imported: `zoneName` /
`subZoneName` into the zone domain, `crop` into the not-yet-existing garden
domain. `SubZone.soilProfileName` is the reverse edge, maintained by the
application layer.

The seventeen `NutrientReading` rows an FGL tomato panel produces are the four +
seven + six slots above, and the seventeen `ReportedOptimum` rows beside them are
the optimum range the report printed for each. **That count was exactly what
Phase 1 was about:** the three grouping read models named a closed list and
`assemblePanel` filled each slot with `byName.get(...)`, so a lettuce panel with
a different row set landed as nulls in a graph whose invariants forbid them.
Every slot is now `Optional`, and an empty one means "the lab did not run that
row" — never zero.

## What each phase changed

Same graph, only the touched types. Phases 1–5 are implemented; the diagram
below is the record of what each one did.

```mermaid
classDiagram
    direction TB

    class PrimaryNutrients {
        <<Phase 1 — D-1>>
        Optional~NutrientReading~ per slot
        or category-keyed collection
    }

    class LabAnalysisInfo {
        <<Phase 2>>
        nullable DepthInches sampleDepth
        nullable SamplingProtocol samplingProtocol
    }

    class SamplingProtocol {
        <<ValueObject — new, Phase 2>>
        subsample count
        tool
        compositing method
        depth stays on the header
    }

    class SoilPhysicalCharacteristics {
        <<Phase 3>>
        NumericNamedValue sar
    }

    class CationBaseSaturation {
        <<Phase 3>>
        censoring marker on hydrogenPct
    }

    class LabAnalysis {
        <<Phase 4>>
        ReportedOptimumCollection reportedOptima
    }

    class ReportedOptimum {
        <<Entity — new, Phase 4>>
        ReportedOptimumId id
        NutrientName nutrientName
        LabAnalysisId labAnalysisId
        OptimumRange range
        MeasurementUnit unit
    }

    class OptimumRange {
        <<sealed ValueObject — new, Phase 4>>
        Closed
        UpperBounded
        LowerBounded
        NotApplicable
    }

    class ReportedRecommendation {
        <<Entity — new, Phase 5>>
        RecommendedInputName inputName
        RecommendedAmount amount
        MeasurementUnit unit
        nullable ApplicationRoute route
    }

    class RecommendedAmount {
        <<sealed ValueObject — new, Phase 5>>
        Quantity
        None
        BelowDetectionLimit
    }

    LabAnalysisInfo *-- "0..1" SamplingProtocol
    SoilPhysicalCharacteristics *-- "1" CationBaseSaturation
    LabAnalysis *-- "0..*" ReportedOptimum : sibling of the panel
    ReportedOptimum *-- "1" OptimumRange
    ReportedOptimum ..> LabAnalysisInfo : labAnalysisId + nutrientName unique
    LabAnalysis *-- "0..*" ReportedRecommendation : sibling of the panel
    ReportedRecommendation *-- "1" RecommendedAmount
```

Read the Phase 4 edge literally: `ReportedOptimum` hangs off `LabAnalysis`
beside the panel, **not** as a component of `NutrientReading`. Putting a range
on the reading fuses measurement with interpretation and violates §18.

Phases 7 and 8 do not appear because they add nothing to this graph. Phase 7 moves constraints
into the read model without changing its components; Phase 8's `CropProfile`
lives in a peer module and applies over this graph rather than joining it.

Phase 0 (the D-2 collapse) changes no type at all — it deletes rows. Two
`SoilProfileInfo`s instead of four, `subZoneName` null on both, one
`LabAnalysis` each.

---

## Gate: two decisions to make before any session starts

Neither is a coding decision, and a session handed the brief will pick one
silently. Decide first, record the choice in the phase, then start.

> **Decided 2026-08-14.**
> **D-1 — `Optional` slots.** As recommended below.
> **D-2 — neither option: collapse the backyard.** See the revised Phase 6.

**D-1 — Panel shape.** Fixed `PrimaryNutrients` / `SecondaryNutrients` /
`MicroNutrients` slots go `Optional`, *or* the three read models give way to a
category-keyed collection.

The trade: `Optional` is a small diff and preserves the named-accessor
ergonomics the console templates already use, but keeps a closed list that
must be edited whenever a lab changes a panel. The category-keyed collection
matches the open, slug-keyed grain already chosen for `NutrientReading` and
survives new labs, but every template touching `panel.primary().nitrateN()`
changes.

Recommendation: `Optional` now, category-keyed later if a second lab or a
third panel appears. Reversible, and the deadline is real.

**D-2 — Backyard trio.** Model the shared sample explicitly
(`LabSample` entity, migration), or surface it in the view only.

Recommendation: view-only for now. The model duplication is honest enough as
long as the UI never implies three independent measurements, and a `LabSample`
migration is not something to run the week new data arrives.

**Decided: collapse instead.** Both options above answer the wrong question.
Three profiles exist because the sub-zones were modelled as soil profiles; the
sample was never split, and there will never be budget to split it. The
sub-zones are crop rows — a planting concern, not a soil-sampling one. So
`backyard-north` / `-center` / `-south` become one zone-scoped `backyard`
profile carrying the single `CH 2671853-002` analysis, and the duplicated rows
(three identical physical-characteristics rows, 51 of 68 nutrient readings)
leave the catalog. This deletes the problem Phase 6 was going to describe, and
`SubZone.soilProfileName` is already `@Nullable`, so the crop-row sub-zones
simply carry no profile.

Done first, before Phases 1–3: it halves the fixture surface those phases
migrate.

---

## Phase 1 — Panel tolerance (blocks August ingest)

**Brief §5.1. Depends on D-1. Implemented 2026-08-14.**

The August report cannot be loaded at all if FGL's lettuce panel differs from
the seventeen tomato-panel rows. This is the only phase with a hard external
deadline.

- Apply D-1 to `PrimaryNutrients`, `SecondaryNutrients`, `MicroNutrients`.
- Relax the `namedEntity(...)` invariants correspondingly.
- `SoilProfileFactory.assemblePanel` already uses `byName.get(...)` and yields
  null for a missing slot — under `Optional` that becomes the intended path,
  not a latent bug.
- Both existing fixtures stay complete (four before the Phase 0 collapse); add
  one deliberately-incomplete analysis to prove assembly survives it. It is
  built in the factory test rather than added to the shared JSON catalog —
  there is no real incomplete report until August, and the catalog holds real
  measurements only.
- Console templates must render an absent nutrient as absent, distinct from a
  present zero.

**Done when:** an analysis missing a nutrient assembles, renders, and round-trips.

---

## Phase 2 — Analysis provenance: depth and sampling protocol

**Brief §5.2. Independent of Phase 1. Implemented 2026-08-14.**

- Add `@Nullable DepthInches sampleDepth` to `LabAnalysisInfo`
  (`kernels/measurements`, already a declared dependency, currently unused by
  soil-api).
- Add `SamplingProtocol` value object in `com.naturalist.soil.observation`:
  ~~depth,~~ subsample count, tool, compositing method. Nullable on the record —
  the March analyses have no protocol and must not be back-filled with
  invented values.
  - **Deviation:** depth is *not* duplicated inside `SamplingProtocol`. It is
    what the lab prints on the report, so it stays on the header as
    `sampleDepth`; carrying it in both places lets the printed depth and the
    intended depth disagree with no way to tell which is true.
- Migrate both existing fixtures with `sampleDepth: null`, matching the
  reports' printed `Depth: N/A`.

Nullable rather than required is the point: absence is the historical truth
for March and needs to stay visible.

**Done when:** August's analysis can record twelve inches and a protocol while
March's records neither.

---

## Phase 3 — Fidelity gaps

**Brief §5.3, §5.4. Independent; can run alongside Phase 2. Implemented 2026-08-14.**

> **Confirmed against the PDFs** (`domains/soil/FGLDocCH_2671853.pdf`, read
> during Phase 4): both reports print `CEC - Hydrogen   < 1.00 %` against an
> optimum of `0.0 - 3.0`. Both rows are correctly stored as censored. The SAR
> values 0.3 and 0.4 are confirmed on the same pages.

- Add SAR to `SoilPhysicalCharacteristics` as a typed
  `NumericNamedValue` beside EC. Update both fixtures from the March PDF
  (0.3 for box1, 0.4 for backyard).
- Represent the censored `CEC-Hydrogen < 1.00`. Minimal form: a censoring
  marker on that component. Do not reach for a general sealed `Quantity` yet —
  gate that on the second censored value actually needing it (FU-3 discipline).
- Leave Lime Requirement, Gypsum Requirement, texture class and the
  Fertilization Recommendations table alone until Phase 5.

**Done when:** the base-saturation sum check can distinguish "hydrogen is
1.00" from "hydrogen is below the detection limit of 1.00".

---

## Phase 4 — `ReportedOptimum` (the golden-master data)

**Brief §2. Depends on Phase 1 — both touch `observation` assembly.
Implemented 2026-08-14.**

> **Finding.** The golden-master test passes, and it says something stronger than
> expected: FGL's exchangeable-cation optima are *not crop-specific at all*. All
> sixteen printed bounds across both analyses are reproduced by four
> crop-invariant base-saturation windows (Ca 60–80%, Mg 10–20%, K 1.0–6.0%,
> Na 0.0–5.0%) projected through each sample's own CEC, to the two significant
> figures the report carries. `FglReplicaStrategy` inherits an exact target for
> those four rows.
>
> `LowerBounded` is implemented but unused — no March row prints a floor-only
> range, contrary to the "all four shapes appear" note below.

- `OptimumRange` sealed value: `Closed`, `UpperBounded`, `LowerBounded`,
  `NotApplicable`. All four shapes appear on the March reports.
- `ReportedOptimum` entity in `com.naturalist.soil.observation`, keyed
  `(nutrientName, labAnalysisId)` unique — same shape as
  `NutrientReadingTestEntitySource`.
- Repository (package-private), query (public, `forLabAnalysisId`),
  collection, `TestEntitySource`, JSON catalog.
- Transcribe all optimum ranges from both March reports into fixtures.
- Assemble onto `LabAnalysis` as a sibling of the panel — **not** as a
  component of `NutrientReading`, which would violate §18.

**Done when:** the March reports round-trip with their printed ranges, and a
test asserts base saturation bounds (Ca 60–80, Mg 10–20, K 1–6, Na 0–5)
projected through each sample's CEC reproduce the stored exchangeable ranges.

That test is the deliverable. It is the reason to store the ranges at all.

---

## Phase 5 — Reported recommendations *(optional)*

**Brief §5.4. Implemented 2026-08-14** — built on request despite the deferral
advice below, which stands unchanged as advice.

Same shape as Phase 4: Lime Requirement, Gypsum Requirement, and the
~~thirteen~~ **twelve**-row Fertilization Recommendations table as append-only
report facts, sibling to the analysis. `None` is a value, not an absent row.

Defer unless an intervention model is actually coming. The data is on paper
and does not expire.

**What the reports actually carry.** Fourteen rows per analysis: twelve
fertilisation rows in lbs/1000 ft² with a `via` route, and two requirement rows
in tons/acre-foot with no route and a `---` optimum. Three amount shapes appear,
and the report distinguishes all three on one page:

| Printed | Modelled as | Means |
|---|---|---|
| `11.2` | `RecommendedAmount.Quantity` | apply this much |
| `None` | `RecommendedAmount.None` | the lab advises applying none |
| `0 Tons/AF` | `Quantity(0)` | the computed requirement came out at zero |
| `< 0.50 Tons/AF` | `RecommendedAmount.BelowDetectionLimit` | below what the method resolves |

Box 1 prints `Lime Requirement 0` and `Lime — None` on the same page, which is
why `Quantity(0)` and `None` must stay distinguishable.

`RecommendedInputName` is a separate vocabulary from `NutrientName`, not a reuse
of it: you apply *nitrogen* and measure the *nitrate* fraction, `sulfur` is not
`sulfate`, `lime` is not a nutrient, and no recommendation splits calcium into
exchangeable and soluble.

### FU-3 gate: the second censored value has arrived

Phase 3 deliberately used a boolean marker for `CEC-Hydrogen < 1.00` rather than
a general censored-quantity type, gating the general shape on *a second censored
value actually needing it*. Gypsum Requirement `< 0.50 Tons/AF` is that second
value, and the gate is now open.

It is **not** closed in this phase, on purpose. The two censored values do not
want the same type yet: hydrogen's censoring needs saturation-sum bounds, the
gypsum requirement's needs a `None` sibling, and a shared `Quantity` that serves
both is a design question rather than a rename. Unifying them is worth doing
when a third case appears or when interpretation starts consuming both — and it
should be its own change, not a rider on a phase.

---

## Phase 6 — Backyard collapse *(done up front, not here)*

**Brief §7. D-2 resolved by collapse.**

Moved ahead of Phase 1 and executed as Phase 0. `backyard-north` /
`backyard-center` / `backyard-south` become a single zone-scoped `backyard`
profile; the two duplicate analyses, their two duplicate
physical-characteristics rows, and 34 duplicate nutrient readings are deleted
from the fixtures. `TestSoilIdentifiers.SoilProfiles.BackyardNorth` becomes
`Backyard`; `BackyardCenter` and `BackyardSouth` are removed.

**Done when:** two profiles, two analyses, 34 readings — and no page can be
mistaken for an independent measurement, because no such profile exists.

---

## Phase 7 — Presentation rules

**Brief §8. Depends on Phases 4 and 6.**

Push the constraints into the read model rather than the templates: band
position within a band is unknown, `None` is distinct from missing, source
labels travel with any assessment. The console is read-only today, so this is
a low-risk phase — but it should follow Phase 4 so there is something to
label.

Explicitly out of scope until then: trend rendering. With two points per
profile there is nothing to trend, and building the chart now guarantees a
regression line appears the moment there are three.

---

## Phase 8 — `agronomy` module and strategies

**Brief §3, §4, §9. Blocked.**

Requires `CropProfile`, which requires the garden/crop domain, which is not
started. Do not begin this inside `soil-api` as a stopgap — the peer-module
rule in §10 is the whole argument, and a temporary home becomes permanent.

Prerequisite chain: garden module → `CropProfile` → `agronomy` module →
`NutrientAssessmentStrategy` → `FglReplicaStrategy` → sufficiency and
saturated-media strategies.

Phase 4's golden-master test is the honest substitute in the meantime, and it
lives entirely inside soil.

---

## Ordering summary

```
D-1 Optional, D-2 collapse  (decided 2026-08-14)
   │
   └── Phase 0  backyard collapse    ✓ shipped (was Phase 6)
          │
          ├── Phase 1  panel tolerance      ✓ shipped
          ├── Phase 2  depth + protocol     ✓ shipped
          └── Phase 3  SAR + censoring      ✓ shipped
                 │
                 └── Phase 4  ReportedOptimum + golden-master test  ✓ shipped
                        │
                        ├── Phase 5  reported recommendations  ✓ shipped
                        └── Phase 7  presentation rules
                               │
                               └── Phase 8  agronomy (blocked on garden domain)
```

Phases 1–3 are the ones with a calendar attached. Everything below Phase 4 can
wait for the fall data to actually land, and will be better informed by it —
particularly the crop-invariance question, which Phase 4's test answers on
first contact with the lettuce panel.
