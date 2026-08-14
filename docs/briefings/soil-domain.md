# soil-domain — Chat Briefing

**Purpose.** Domain vocabulary plus current shape of the soil module (two
built sub-contexts: profile and observation; one modelled-but-unwired: event),
sized for a chat Claude session. Pair with
`docs/briefings/framework-core.md` (framework / structural glue) and
`docs/briefings/shared-kernels.md` when the task touches `measurements`.    

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-08-10), not
extrapolated. If you need a type not listed here, ask before inventing one.

**Source documents.** Two PDFs sit in `domains/soil/` and are the provenance
for every fixture value: `FGLDocCH_2671853.pdf` (the March 2026 Fruit Growers
Laboratory report) and `soil science report.pdf` (the agronomic
interpretation). Values in this briefing are transcribed from those, not
invented.

---

## 1. Module Scope and DAG

```
soil-api  →  framework, identifiers, measurements
```

`soil-api` depends on exactly one kernel beyond the standard set:

- `kernels/measurements` — `DepthInches`, `ElectricalConductivity`,
  `PrecipitationInches`, `AreaSquareFeet`. The physically-general
  measurement types; soil-specific ones stay in soil-api (§7).

**Two standard kernels soil does *not* use:**

- **`field-notes`** — no soil type carries a `Description`. Soil is
  measurement data, not a Durrell field description. Do not add one.
- **`taxonomy`** — soil is not an organism domain. No
  `TaxonomicClassification`.

No cross-domain api dependencies. Every cross-domain reference is a typed
name from `domains/identifiers` (§13).

Maven modules under `domains/soil/`:

```
soil-api              — the domain model (this briefing's subject)
soil-core             — query adapters + SoilProfileFactory
soil-repository-test  — mocks, contract tests, TestEntitySources, JSON fixtures
soil-repository-rdms  — pom only; no sources yet
soil-test-context     — SoilTestContext (manual DI)
soil-console          — read-only Spring/JTE viewer at /soil
```

---

## 2. Package Map

```
com.naturalist.soil/
  SoilProfileInfo                    — NamedEntity<SoilProfileName>, @AggregateRoot
  SoilProfileInfoRepository          — package-private (N=1 collapse)
  SoilProfileInfoQuery               — public (N=1 collapse)
  SoilProfileInfoCollection          — BehavioralCollection<SoilProfileInfo>
  SoilProfile                        — ReadModel (assembled; never stored)
  SoilProfileQuery                   — public, standalone (not EntityQuery)
  SoilPH                             — NumericNamedValue
  MulchLayer                         — ValueObject (modelled, unreferenced)
  MulchType                          — enum (modelled, unreferenced)

com.naturalist.soil.observation/
  LabAnalysisInfo                    — Entity<LabAnalysisId>
  LabAnalysisInfoRepository          — package-private
  LabAnalysisInfoQuery               — public
  LabAnalysisInfoCollection          — BehavioralCollection<LabAnalysisInfo>

  NutrientReading                    — Entity<NutrientReadingId>
  NutrientReadingRepository          — package-private
  NutrientReadingQuery               — public
  NutrientReadingCollection          — BehavioralCollection<NutrientReading>

  SoilPhysicalCharacteristics        — Entity<SoilPhysicalCharacteristicsId>
  SoilPhysicalCharacteristicsRepository   — package-private
  SoilPhysicalCharacteristicsQuery        — public
  SoilPhysicalCharacteristicsCollection   — BehavioralCollection<...>

  LabAnalysis                        — ReadModel (assembled)
  NutrientPanel                      — ReadModel (assembled)
  PrimaryNutrients                   — ReadModel (assembled)
  SecondaryNutrients                 — ReadModel (assembled)
  MicroNutrients                     — ReadModel (assembled)
  CationBaseSaturation               — ValueObject
  CecMeqPer100g, LimestonePct, SaturationPct  — NumericNamedValue
  Nutrients                          — final utility class (nutrient catalog)
  NutrientCategory, MeasurementUnit  — enums

com.naturalist.soil.event/            ← modelled, NOT wired (§9)
  AmendmentEvent                     — Entity<AmendmentEventId>
  IrrigationEvent                    — Entity<IrrigationEventId>
  TillageEvent                       — Entity<TillageEventId> (+ nested TillageType enum)
  PrecipitationEvent                 — Entity<SoilPrecipitationEventId>
  AmendmentRate                      — NumericNamedValue
  AmendmentUnit                      — enum
```

### Namespace shape — no namespace wrappers anywhere

There is **no** `SoilRepository` / `SoilQuery` / `SoilEntityCollections`
namespace class. The root package applies the ADR-020 N=1 collapse
(one entity → top-level package-private repository, top-level public query).

`observation` holds **three** entities and still uses three top-level
package-private repository interfaces rather than an
`ObservationRepository` namespace class. That is the current shape —
match it; do not introduce a namespace wrapper unless the task explicitly
asks for that refactor.

### Identifier locations (in the `identifiers` module, not in soil-api)

| Type                            | Package                          | Branch       | maxLength |
|---------------------------------|----------------------------------|--------------|-----------|
| `SoilProfileName`               | `com.naturalist.soil`            | `EntityName` | 64        |
| `CropName`                      | `com.naturalist.soil`            | `EntityName` | 48        |
| `NutrientName`                  | `com.naturalist.soil.observation`| `EntityName` | 48        |
| `LabAnalysisId`                 | `com.naturalist.soil.observation`| `EntityId`   | —         |
| `NutrientReadingId`             | `com.naturalist.soil.observation`| `EntityId`   | —         |
| `SoilPhysicalCharacteristicsId` | `com.naturalist.soil.observation`| `EntityId`   | —         |
| `AmendmentEventId`              | `com.naturalist.soil.event`      | `EntityId`   | —         |
| `IrrigationEventId`             | `com.naturalist.soil.event`      | `EntityId`   | —         |
| `TillageEventId`                | `com.naturalist.soil.event`      | `EntityId`   | —         |
| `SoilPrecipitationEventId`      | `com.naturalist.soil.event`      | `EntityId`   | —         |

Every `EntityId` subclass exposes `of(UUID)` (`@JsonCreator`) and
`create()` (generates a UUIDv7 via `EntityId.newUUID()`).

**Naming trap.** The identity types are named for the *concept*, not the
record. `SoilProfileInfo` is keyed by `SoilProfileName`; `LabAnalysisInfo`
by `LabAnalysisId`. There is no `SoilProfileInfoName` or
`LabAnalysisInfoId`.

---

## 3. The `*Info` / bare-noun convention

This is the soil domain's most distinctive naming rule and it applies
throughout:

| Suffix       | Meaning                                                              |
|--------------|----------------------------------------------------------------------|
| `*Info`      | The **persisted fact**. An `Entity` or `NamedEntity`. Has a repository, a query, a collection, a `TestEntitySource`, and a JSON catalog. |
| bare noun    | The **assembled `ReadModel`**. Composed on read by a factory from persisted parts. Never stored, no repository, no JSON. |

- `SoilProfileInfo` (persisted root) → `SoilProfile` (assembled view).
- `LabAnalysisInfo` (persisted header) → `LabAnalysis` (assembled
  header + panel + physical characteristics).

`NutrientPanel`, `PrimaryNutrients`, `SecondaryNutrients`,
`MicroNutrients` are also assembled `ReadModel`s — they compose
`NutrientReading` entities and are never persisted in that shape.

---

## 4. Entity Summary

| Type                          | Identity                        | Branch                            | DDD role                          |
|-------------------------------|---------------------------------|-----------------------------------|-----------------------------------|
| `SoilProfileInfo`             | `SoilProfileName`               | `NamedEntity` (slug)              | Aggregate root — spatial anchor   |
| `LabAnalysisInfo`             | `LabAnalysisId`                 | `Entity` (UUIDv7)                 | Lab report header                 |
| `NutrientReading`             | `NutrientReadingId`             | `Entity` (UUIDv7)                 | One nutrient value, one analysis  |
| `SoilPhysicalCharacteristics` | `SoilPhysicalCharacteristicsId` | `Entity` (UUIDv7)                 | Physical/derived properties, 1:1 with an analysis |
| `SoilProfile`                 | (none)                          | `ReadModel`                       | Profile + its analyses            |
| `LabAnalysis`                 | (none)                          | `ReadModel`                       | Header + panel + characteristics  |
| `NutrientPanel`               | (none)                          | `ReadModel`                       | Nutrients bucketed by category    |
| `Primary/Secondary/Micro`     | (none)                          | `ReadModel`                       | One FGL report section each       |
| `CationBaseSaturation`        | (none)                          | `ValueObject`                     | % base saturation block           |
| `MulchLayer`                  | (none)                          | `ValueObject`                     | Surface mulch snapshot (unwired)  |
| `AmendmentEvent` and siblings | `*EventId`                      | `Entity` (UUIDv7)                 | Management history (unwired)      |

### The FK chain

```
SoilProfileInfo  (name: SoilProfileName)
      ▲
      │ soilProfileName
LabAnalysisInfo  (id: LabAnalysisId)
      ▲                          ▲
      │ labAnalysisId            │ labAnalysisId (unique — 1:1)
NutrientReading            SoilPhysicalCharacteristics
```

All FKs point **upward, one level only**. There is no skip-level reference
(a `NutrientReading` does not carry `soilProfileName`), and there are no
downward collections on the persisted records — children are gathered by
reverse lookup at assembly time.

---

## 5. SoilProfileInfo — the aggregate root

```java
@AggregateRoot
public record SoilProfileInfo(
    SoilProfileName name,
    ZoneName zoneName,
    @Nullable SubZoneName subZoneName
) implements NamedEntity<SoilProfileName>
```

The identity and spatial anchor of a managed soil unit. Behavior method:
`isSubZoneScoped()` → `subZoneName != null`.

Invariants: `entityName(name)`, `entityName(zoneName)`,
`entityNameOrNull(subZoneName)`.

**Spatial association rule.** `zoneName` is always present.
`subZoneName` is null when the profile covers a whole Zone, set when it
covers a SubZone subdivision.

`ZoneName` and `SubZoneName` come from `domains/identifiers`
(`com.naturalist.zone` / `com.naturalist.zone.subzone`) — soil-api has
**no compile-time dependency on zone-api**. The reverse pointer
(`SubZone.soilProfileName`) is likewise a soft `SoilProfileName` reference
held in zone-api.

No `with*` methods — nothing on the root is mutable.

---

## 6. Observation sub-context — the measurement grain

The governing rule for this whole sub-context:

> **A measurement is a fact. Optimum ranges and statuses are
> interpretation.** Nothing in `observation` stores a target, a threshold,
> a band, or a status. Those are derived by applying a `CropProfile`
> (a future effort) over these values.

### `LabAnalysisInfo` — the header

```java
public record LabAnalysisInfo(
    LabAnalysisId id,
    SoilProfileName soilProfileName,
    CropName crop,
    LocalDate sampleDate,
    String labId,
    String labSampleId,
    @Nullable String notes
) implements Entity<LabAnalysisId>
```

Immutable, append-only — a lab report is a historical fact. `crop` is a
`CropName` soft reference naming the *interpretation context* (which
crop's optimum ranges apply); the rich crop-planting concept is a separate
effort. Current lab: Fruit Growers Laboratory (FGL), Chico CA; sample IDs
`CH XXXXXXX-NNN`.

Invariants: `entityId(id)`, `entityName(soilProfileName)`,
`entityName(crop)`, `notNull(sampleDate)`, `notNull(labId)`,
`notNull(labSampleId)`.

### `NutrientReading` — the fine grain

```java
public record NutrientReading(
    NutrientReadingId id,
    NutrientName nutrientName,
    LabAnalysisId labAnalysisId,
    BigDecimal value,
    MeasurementUnit unit
) implements Entity<NutrientReadingId>
```

One row per nutrient per analysis — the grain that makes "monitor a chosen
set of nutrients over time" a direct query. Identity is the surrogate
`NutrientReadingId`; the **logical key `(nutrientName, labAnalysisId)` is
enforced as a declared unique constraint**, because the framework offers
only single-slug or single-UUID identity (no composite natural key).

`nutrientName` is a slug rather than an enum deliberately: a lab reporting
a nutrient outside the known catalog still round-trips as a reading.

`unit` is explicit here (values are homogeneous BigDecimals) and absent
from `SoilPhysicalCharacteristics` (values are typed).

### `SoilPhysicalCharacteristics` — the coarse grain, richly typed

```java
public record SoilPhysicalCharacteristics(
    SoilPhysicalCharacteristicsId id,
    LabAnalysisId labAnalysisId,
    CecMeqPer100g cecMeqPer100g,
    SoilPH pH,
    ElectricalConductivity ecDsPerMeter,
    LimestonePct limestonePct,
    SaturationPct saturationPct,
    CationBaseSaturation cationBaseSaturation
) implements Entity<SoilPhysicalCharacteristicsId>
```

One row per analysis; `labAnalysisId` is enforced unique. A **separate
entity family** from nutrients by design: nutrients are many, homogeneous,
and lab-variable (open name-keyed grain with an explicit unit); physical
characteristics are a small standard set with heterogeneous typed values,
so they keep their measurement types and the unit is intrinsic to the type.

Invariants use `namedValue(...)` for the five `NumericNamedValue`
components and `valueObject(cationBaseSaturation)`.

---

## 7. Nutrient assembly — panel read models and the catalog

### `NutrientPanel` (ReadModel)

```java
public record NutrientPanel(
    PrimaryNutrients primary,
    SecondaryNutrients secondary,
    MicroNutrients micro
) implements ReadModel
```

The three groups mirror the FGL report's sections exactly:

| Group                | Components (all `NutrientReading`)                                                                  |
|----------------------|-----------------------------------------------------------------------------------------------------|
| `PrimaryNutrients`   | `nitrateN`, `phosphorusP2O5`, `potassiumExch`, `potassiumSoluble`                                    |
| `SecondaryNutrients` | `calciumExch`, `calciumSoluble`, `magnesiumExch`, `magnesiumSoluble`, `sodiumExch`, `sodiumSoluble`, `sulfate` |
| `MicroNutrients`     | `zinc`, `manganese`, `iron`, `copper`, `boron`, `chloride`                                           |

Seventeen readings per analysis. Every component is asserted non-null via
`namedEntity(...)` — an analysis missing a nutrient fails the invariant
walk when the profile is assembled. Fixtures must be complete.

**Exchangeable vs soluble.** Ca, Mg, K and Na are each reported as two
fractions: *exchangeable* (held on soil colloid surfaces, the reserve) and
*soluble* (dissolved in soil water, plant-available now). They are separate
readings with separate slugs, not one value.

### `Nutrients` — the canonical catalog (final utility class, soil-api)

Holds the 17 `NutrientName` constants and a static
`Map<NutrientName, NutrientCategory>`. Two static methods:

- `categoryOf(NutrientName)` — throws `IllegalArgumentException` for an
  unknown nutrient.
- `isKnown(NutrientName)` — whether the nutrient has a category.

Slugs: `nitrate-n`, `phosphorus-p2o5`, `potassium-exchangeable`,
`potassium-soluble`, `calcium-exchangeable`, `calcium-soluble`,
`magnesium-exchangeable`, `magnesium-soluble`, `sodium-exchangeable`,
`sodium-soluble`, `sulfate`, `zinc`, `manganese`, `iron`, `copper`,
`boron`, `chloride`.

`NutrientCategory` — `PRIMARY`, `SECONDARY`, `MICRO`.
`MeasurementUnit` — one constant today, `LBS_PER_1000_SQFT`
(symbol `"lbs/1000 ft²"`), the unit FGL reports soil nutrients in.

---

## 8. Typed measurement values

### Soil-specific `NumericNamedValue`s (in soil-api)

| Type              | Package       | Scale | Valid range      | Meaning                                    |
|-------------------|---------------|-------|------------------|--------------------------------------------|
| `SoilPH`          | `soil`        | 2     | 0 ≤ v ≤ 14       | pH, log scale                              |
| `CecMeqPer100g`   | `observation` | 1     | v > 0            | Cation exchange capacity, meq/100 g        |
| `LimestonePct`    | `observation` | 1     | v ≥ 0            | CaCO₃ as % dry weight                      |
| `SaturationPct`   | `observation` | 1     | v > 0            | Gravimetric water content at paste saturation |
| `AmendmentRate`   | `event`       | 2     | v > 0            | lbs/1000 sqft                              |

All use `RoundingMode.HALF_UP`, expose a `@JsonCreator public static of(BigDecimal)`,
and override `scale()` / `roundingMode()` / `isValid()`. `LimestonePct`
adds one behavior method: `isAbove(BigDecimal threshold)` (exclusive,
compares normalized values).

Scale is not decoration — it reflects FGL reporting precision, so `7.20`
round-trips as `7.20`.

### From `kernels/measurements`

`ElectricalConductivity` (dS/m), `DepthInches`, `PrecipitationInches`,
`AreaSquareFeet`. Generic physical quantities live in the kernel; the
FGL-specific ones above stay in soil-api.

### `CationBaseSaturation` (ValueObject)

```java
public record CationBaseSaturation(
    BigDecimal calciumPct,
    BigDecimal magnesiumPct,
    BigDecimal potassiumPct,
    BigDecimal sodiumPct,
    BigDecimal hydrogenPct
) implements ValueObject
```

How the sample's CEC is occupied across the exchangeable cations. Modelled
as one cohesive value object rather than five loose fields (ADR-013) —
the five percentages partition the exchange sites and sum to ≈ 100%.
Each is `notNull` + `inRange(0, 100)`.

**Do not confuse** with `SoilPhysicalCharacteristics.saturationPct()`,
which is the physical water/paste saturation (a texture and drainage
indicator) — a completely different measurement.

---

## 9. Event sub-context — modelled, NOT wired

`com.naturalist.soil.event` contains four `Entity` records with rich
javadoc and behavior, and **nothing else**: no repository, no query, no
collection, no `TestEntitySource`, no JSON catalog, no assembly into
`SoilProfile`. Nothing outside the package references these types. Treat
them as a designed-but-dormant sub-context.

| Record               | Identity                   | Key components                                                                                             |
|----------------------|----------------------------|------------------------------------------------------------------------------------------------------------|
| `AmendmentEvent`     | `AmendmentEventId`         | `zoneName`, `@Nullable subZoneName`, `compoundName` (chemistry FK), `amount` (`AmendmentRate`), `unit`, `appliedDate`, `@Nullable notes` |
| `IrrigationEvent`    | `IrrigationEventId`        | `zoneName`, `@Nullable subZoneName`, `volumeGallons`, `appliedDate`, `leachingIrrigation` (boolean), `@Nullable notes` |
| `TillageEvent`       | `TillageEventId`           | `zoneName`, `@Nullable subZoneName`, `tillageDate`, `depthInches`, `tillageType`, `@Nullable notes`         |
| `PrecipitationEvent` | `SoilPrecipitationEventId` | `zoneName`, `@Nullable subZoneName`, `startDate`, `endDate`, `totalInches`, `totalDuration`, `peakIntensityInchesPerHour`, `@Nullable weatherEventName`, `@Nullable notes` |

Behavior methods that carry real domain knowledge:

- `TillageEvent.estimatedRecoveryDays()` — switch on
  `TillageType`: `BROADFORK` → 5; `HAND_CULTIVATION` → 7 (≤3 in) else 12;
  `ROTOTILL` → 12 (≤4 in) else 21. Nested enum `TillageType`:
  `ROTOTILL`, `BROADFORK`, `HAND_CULTIVATION`.
- `PrecipitationEvent.isSignificantLeachingEvent()` — total ≥ 0.5 in
  **and** peak intensity ≤ 0.3 in/hr, so water infiltrates rather than
  runs off. Also `estimatedVolumeGallons(areaSqft)` (0.623 gal per inch
  per sqft), `isMultiDay()`, `isCorrelated()`.

**Local-copy pattern.** `soil.event.PrecipitationEvent` is the soil
domain's *own* record of a precipitation event, distinct from
`com.naturalist.weather.PrecipitationEvent`. Each domain that cares about
precipitation keeps its own copy with its own context; the
`@Nullable weatherEventName` (`com.naturalist.weather.PrecipitationEventId`)
is the cross-domain correlation key. This is why the id type is
`SoilPrecipitationEventId`, not `PrecipitationEventId`.

`AmendmentUnit` — `POUNDS`, `GRAMS`, `FLUID_OUNCES`, `GALLONS`,
`TABLESPOONS`. Weight for granular/powder amendments, volume for liquids.

---

## 10. MulchLayer / MulchType — modelled, unreferenced

`MulchLayer` (ValueObject: `mulchType`, `depthInches`, `appliedDate`,
`@Nullable replacedDate`, `@Nullable notes`; behavior `isActive()`) and
`MulchType` (enum) sit in the soil root package but are **not a component
of any record** — `SoilProfile` does not carry a current mulch layer.

`MulchType` permits: `STRAW`, `PINE_NEEDLE`, `WOOD_CHIP`, `WOOL`,
`BURLAP_COLLAR`, `BARE`. Behavior: `isAcidifying()` (true only for
`PINE_NEEDLE`), `contributesNitrogen()` (true only for `WOOL`, ~10% N by
dry weight).

**The mulch → thrips bridge is an application-layer concern.** Each
`MulchType` implies a `ThripsHabitatRisk` (a zone-api type), but the
mapping lives in the application layer because soil-api and zone-api are
peer modules and neither may depend on the other. zone-api's `SubZone` and
`ThripsHabitatRisk` reference mulch **in javadoc prose only** — there is no
import. Keep it that way.

---

## 11. Query / Repository / Collection Surface

### `SoilProfileQuery` (public, standalone)

```java
public interface SoilProfileQuery {
    Optional<SoilProfile> getBySoilProfileName(SoilProfileName soilProfileName);
}
```

Does **not** extend `EntityQuery` — the profile is assembled, not stored.
Mirrors the insects `TaxonViewQuery` aggregate-read pattern.

### The three `EntityQuery` ports

```java
public interface SoilProfileInfoQuery
    extends EntityQuery<SoilProfileName, SoilProfileInfo, SoilProfileInfoCollection>
// no additional methods

public interface LabAnalysisInfoQuery
    extends EntityQuery<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoCollection> {
    LabAnalysisInfoCollection forSoilProfileName(SoilProfileName soilProfileName);
}

public interface NutrientReadingQuery
    extends EntityQuery<NutrientReadingId, NutrientReading, NutrientReadingCollection> {
    NutrientReadingCollection forLabAnalysisId(LabAnalysisId labAnalysisId);
    NutrientReadingCollection forNutrientName(NutrientName nutrientName);
}

public interface SoilPhysicalCharacteristicsQuery
    extends EntityQuery<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics,
                        SoilPhysicalCharacteristicsCollection> {
    Optional<SoilPhysicalCharacteristics> forLabAnalysisId(LabAnalysisId labAnalysisId);
}
```

`EntityQuery<NAME, E, EC>` supplies `getByName(NAME)`,
`findByNameSet(Set<NAME>)`, `findPage(PageRequest) → Page<E>`. The
`Entity`-branch types fill the `NAME` slot with their **id** type — so
`getByName(labAnalysisId)` is correct, if oddly named.

Naming convention: **reverse lookups on a query are prefixed `for*`**
(`forSoilProfileName`, `forLabAnalysisId`, `forNutrientName`); the matching
repository methods are prefixed `getBy*`.

`NutrientReadingQuery.forNutrientName` is the monitoring time-series
lookup — one nutrient across analyses over time.
`SoilPhysicalCharacteristicsQuery.forLabAnalysisId` returns `Optional`
(1:1), the others return collections.

### Repositories (all package-private, all `EntityRepository`)

```java
interface SoilProfileInfoRepository
    extends EntityRepository<SoilProfileName, SoilProfileInfo>

interface LabAnalysisInfoRepository
    extends EntityRepository<LabAnalysisId, LabAnalysisInfo> {
    List<LabAnalysisInfo> getBySoilProfileName(SoilProfileName soilProfileName);
}

interface NutrientReadingRepository
    extends EntityRepository<NutrientReadingId, NutrientReading> {
    List<NutrientReading> getByLabAnalysisId(LabAnalysisId labAnalysisId);
    List<NutrientReading> getByNutrientName(NutrientName nutrientName);
}

interface SoilPhysicalCharacteristicsRepository
    extends EntityRepository<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics> {
    Optional<SoilPhysicalCharacteristics> getByLabAnalysisId(LabAnalysisId labAnalysisId);
}
```

Repositories return raw `List`/`Optional`; the query adapter wraps them in
the `BehavioralCollection`.

### BehavioralCollections

`SoilProfileInfoCollection`, `LabAnalysisInfoCollection`,
`NutrientReadingCollection`, `SoilPhysicalCharacteristicsCollection` —
all `final class extends BehavioralCollection<...>`, package-private
constructor, public `of(Collection<...>)` / `empty()` factories. **No
domain-specific filtering methods on any of them today.**

### Adapters in `soil-core`

`SoilProfileInfoQueryImpl`, `LabAnalysisInfoQueryImpl`,
`NutrientReadingQueryImpl`, `SoilPhysicalCharacteristicsQueryImpl` — all
`@DomainService`, all extend `AbstractEntityQuery<NAME, E, EC, REPO>`,
all thin (observe → delegate). `findByNameSet` is overridden in each:
the `NamedEntity` one validates with `i.entityNameCollection(...)`, the
`Entity` ones with `i.identifierSet(...)`.

`SoilProfileQueryImpl` is **not** `@DomainService` — its
`SoilProfileFactory` is not a Spring bean, so it is wired manually in the
context and never component-scanned.

---

## 12. SoilProfileFactory — assembly (package-private, soil-core)

Concrete class, no interface, no `Impl` suffix (ADR-020), mirroring
`InsectTaxonViewFactory`. Constructor takes the four queries. Per ADR-017
it validates its own arguments with `throwWhenInvalid()` but only
*observes* the assembled profile (`observe(...).observe(Level.WARN)`).

```
buildByName(SoilProfileName)
  → soilProfileInfoQuery.getByName(name)
  → map: new SoilProfile(info, assembleAnalyses(name))

assembleAnalyses
  → labAnalysisInfoQuery.forSoilProfileName(name)
  → per info: new LabAnalysis(info, assemblePanel(readings), physical)

assemblePanel
  → readings collected into Map<NutrientName, NutrientReading>
  → each Primary/Secondary/Micro slot filled by byName.get(Nutrients.CONSTANT)
```

Two consequences worth knowing before you touch fixtures:

- `assemblePanel` uses `byName.get(...)` — a missing nutrient yields a
  `null` slot, which the `PrimaryNutrients`/`SecondaryNutrients`/
  `MicroNutrients` `namedEntity(...)` invariants reject. **All 17
  nutrients must be present per analysis.**
- `assembleAnalysis` uses `physicalCharacteristicsQuery.forLabAnalysisId(id).orElse(null)`,
  and `LabAnalysis.invariants()` asserts
  `namedEntity(physicalCharacteristics)`. **Every analysis needs its
  physical-characteristics row.**

`SoilProfile` itself exposes `soilProfileName()` (delegates to
`info.name()`) and `latestLabAnalysis()` (last element of the list —
analyses are ordered most-recent-last).

---

## 13. Cross-domain References — by typed name only

| Reference                  | Direction            | Type                                     |
|----------------------------|----------------------|------------------------------------------|
| `ZoneName`, `SubZoneName`  | soil → zone          | `EntityName` (from `identifiers`)        |
| `CompoundName`             | soil.event → chemistry | `EntityName` (from `identifiers`)      |
| `PrecipitationEventId`     | soil.event → weather | `EntityId` (from `identifiers`)          |
| `CropName`                 | soil → (future crop domain) | `EntityName` (from `identifiers`) |
| `SoilProfileName`          | zone → soil (reverse)| `EntityName` on `SubZone.soilProfileName`|

Every one of these is a **soft reference through `domains/identifiers`**.
soil-api imports no other domain's api, and no other domain's api imports
soil-api. Referential integrity across these edges is deferred to the
RDBMS layer.

---

## 14. JSON Catalog Locations (soil-repository-test, NOT soil-api)

```
soil-repository-test/src/main/resources/soil/
  profile/soil-profile-info.json               —  4 SoilProfileInfo
  observation/lab-analysis-info.json           —  4 LabAnalysisInfo
  observation/nutrient-reading.json            — 68 NutrientReading (4 × 17)
  observation/soil-physical-characteristics.json —  4 SoilPhysicalCharacteristics
```

No JSON for the event sub-context or for mulch — neither is wired.

Catalog conventions:

- `SoilProfileInfo` entries key on `"name"` (the `SoilProfileName` slug).
- The three `Entity` types key on `"id"` as a **UUIDv7 string**. The
  fixture ids are hand-assigned and deliberately readable — e.g. the
  analysis ids embed the FGL job number: `02671853-0001-7000-8000-…`.
- Remaining fields match record component names exactly
  (`ecDsPerMeter`, `cecMeqPer100g`, `pH`, …).
- `cationBaseSaturation` is a nested object with the five `*Pct` fields.
- `unit` serializes by enum constant name: `"LBS_PER_1000_SQFT"`.
- `subZoneName: null` for the zone-scoped profile.

`TestEntitySource` subclasses live beside them and declare the unique
constraints the framework cannot infer:

| Source                                         | `uniqueConstraints()`             |
|------------------------------------------------|-----------------------------------|
| `SoilProfileInfoTestEntitySource`              | (default — name uniqueness only)  |
| `LabAnalysisInfoTestEntitySource`              | (default)                         |
| `NutrientReadingTestEntitySource`              | `"nutrientName+labAnalysisId"`    |
| `SoilPhysicalCharacteristicsTestEntitySource`  | `"labAnalysisId"`                 |

### `TestSoilIdentifiers` (domains/identifiers-test)

Nested to mirror the object graph:
`SoilProfiles.Box1.{name, LabAnalyses, NutrientReadings, PhysicalCharacteristics}`,
`SoilProfiles.BackyardNorth.{name, LabAnalyses, PhysicalCharacteristics}`,
`SoilProfiles.BackyardCenter.{name, LabAnalyses}`,
`SoilProfiles.BackyardSouth.{name, LabAnalyses}`, plus a top-level
`Nutrients.{CALCIUM_SOLUBLE, BORON}` and a single
`SoilProfiles.NotFound` covering every type
(`soilProfile = "unobtainium-bed"`, `nutrientName = "unobtainium"`, and
fictitious `…-9999-…` UUIDs). Ids match the fixture JSON exactly.

---

## 15. Site Context — Oak Vista (Chico, CA)

All fixture data is real, transcribed from FGL report **CH 2671853**,
sampled **March 3, 2026**, interpreted for crop `tomato`.

### The four profiles

| `SoilProfileName`  | Zone      | SubZone           | Substrate                    | FGL sample      |
|--------------------|-----------|-------------------|------------------------------|-----------------|
| `box1`             | `box-1`   | — (zone-scoped)   | Worm casting blend, raised bed | `CH 2671853-001` |
| `backyard-north`   | `backyard`| `backyard-north`  | Native clay amended          | `CH 2671853-002` |
| `backyard-center`  | `backyard`| `backyard-center` | Native clay amended          | `CH 2671853-002` |
| `backyard-south`   | `backyard`| `backyard-south`  | Native clay amended          | `CH 2671853-002` |

The three backyard sub-zones **share one physical lab sample** but each
carries its own `LabAnalysisInfo` row (distinct `id`, same `labSampleId`)
so each profile assembles independently.

### Physical characteristics as measured

| Property        | Box 1 (-001) | Backyard (-002, all three) |
|-----------------|--------------|-----------------------------|
| CEC (meq/100 g) | 44.9         | 34.2                        |
| pH              | 7.2          | 7.2                         |
| EC (dS/m)       | 0.504        | 0.662                       |
| Limestone (%)   | 1.7          | 2.9                         |
| Saturation (%)  | 126          | 75.3                        |
| Ca / Mg / K / Na / H base saturation (%) | 74.6 / 22.9 / 2.09 / 0.408 / 1.00 | 77.2 / 19.4 / 2.75 / 0.681 / 1.00 |

Box 1's saturation of 126% and CEC of 44.9 reflect the organic
worm-casting/coco-coir blend, not a mineral soil.

### Agronomic context (from the interpretation report — *not* in the code)

- **pH 7.2 is optimal.** Never recommend lime.
- Soluble calcium is very low in both beds — blossom-end-rot risk elevated.
  The gypsum (CaSO₄·2H₂O) rehabilitation program targets this; projected
  3–5 seasons, tracked by an annual March FGL test.
- Boron must accompany gypsum — calcium transport to fruit requires boron
  as a co-factor.
- Limestone at 2.9% in the backyard is the insoluble CaCO₃ reserve
  available for Thiobacillus-mediated conversion to plant-available gypsum.
- Blood meal applied April 2026 to Box 1 exceeded the calculated
  requirement; an April 2026 rain event (1.5 in over 3 days) provided
  natural leaching. Biologically active soils amplify nitrogen release
  1.6–2.0× the mineral-soil baseline.
- The backyard was rototilled to 6 in on April 2, 2026; expected
  biological drainage recovery 2–4 weeks, extended by the clay sublayer.

**None of that interpretation is encoded in soil-api.** Optimum ranges,
statuses, amplification factors and trajectories all belong to the
`CropProfile` / monitoring effort (§17). Do not add them to a
measurement record.

---

## 16. Wiring — test context and console

`SoilTestContext` (`soil-test-context`, package `com.naturalist.soil`)
simulates DI: constructs the four repository mocks and query impls, builds
the `SoilProfileFactory`, and exposes exactly two accessors —
`soilProfileQuery()` and `soilProfileInfoQuery()`. `SoilsTestContextInternal`
in `soil-core/src/test/java` serves core's own tests (the Maven cycle rule).

`SoilsController` (`soil-console`, `@RequestMapping("/soil")`) is
**read-only**: `/soil` redirects to `/soil/profiles` (list, one assembled
`SoilProfile` per row), and `/soil/profiles/{name}` renders the per-profile
detail page — each dated analysis with its nutrient panel and physical
characteristics. It builds its own `NaturalistDatabase` in the constructor
(TODO: becomes a Spring bean when the rdbms adapter lands) and pulls a
`GlossaryLinker` from `LibraryTestContext` so soil-chemistry terms get
inline definition popovers. Templates: `soil/list.jte`, `soil/profile.jte`,
`soil/readingRow.jte`.

---

## 17. Current State — What's Built, What's Not

**Built and stable.**

- `SoilProfileInfo` root with repository, mock, contract test, query,
  collection, `TestEntitySource`, JSON catalog.
- The full observation grain: `LabAnalysisInfo`, `NutrientReading`,
  `SoilPhysicalCharacteristics` — each with repository, mock, behavioral
  contract test, query + adapter, collection, `TestEntitySource`, and real
  FGL fixtures.
- Assembly: `SoilProfileFactory` → `SoilProfile` / `LabAnalysis` /
  `NutrientPanel` / `Primary`+`Secondary`+`Micro`.
- `Nutrients` catalog, `NutrientCategory`, `MeasurementUnit`, and the five
  soil-specific `NumericNamedValue` types.
- `TestSoilIdentifiers`, `SoilTestContext`, `SoilsTestContextInternal`.
- Read-only soil console with glossary auto-linking.

**Modelled but not wired.**

- The whole `event` sub-context — four records, no ports, no fixtures, no
  assembly into `SoilProfile`.
- `MulchLayer` / `MulchType` — no record references them.

**Not built at all.**

- **`CropProfile`** — the optimum-range catalog keyed by `(source, crop)`
  with a descriptive `referenceCec`. This is where every target, band, and
  status belongs. Nothing exists yet.
- **Derived status / assessments** — applying a `CropProfile` to a
  profile's measurements as of a date.
- **`MeasurementSeries`** — the monitoring read model (a chosen set of
  measurements as time series across analyses, with targets and action
  triggers), unifying the nutrient and physical families for display.
- No command surface anywhere in soil — read-only today.
- `soil-repository-rdms` is a pom with no sources.
- No sensor / moisture data in the domain (`MoisturePercent` exists in the
  measurements kernel; soil-api does not use it).
- Ownership: a `Property` aggregate as tenancy root above `Zone` is
  designed but unscheduled; soil is unchanged by it (owner derives
  `zone → property`).

---

## 18. Anti-patterns Specific to soil-api

- **Do not put an optimum range, threshold, target, band, or status on a
  measurement record.** `NutrientReading` and `SoilPhysicalCharacteristics`
  are measured facts. Interpretation is a `CropProfile` applied over them.
  This is the domain's central design decision — violating it is the one
  change most likely to be rejected.
- **Do not add a `unit` component to `SoilPhysicalCharacteristics`.**
  Its values are typed (`ElectricalConductivity` *is* dS/m); the unit is
  intrinsic. Conversely, do not remove `unit` from `NutrientReading` —
  its homogeneous BigDecimals need it.
- **Do not convert `NutrientName` to an enum.** The slug keeps the reading
  grain open to new nutrients and other labs. `Nutrients` is the curated
  catalog, deliberately not exhaustive.
- **Do not add a `Description` to any soil type.** soil-api does not depend
  on `field-notes`. Soil is measurement data.
- **Do not import zone-api, chemistry-api, or weather-api into soil-api.**
  Every cross-domain reference is a typed name from `domains/identifiers`.
  The mulch → `ThripsHabitatRisk` mapping in particular is an
  application-layer concern, not a soil-api import.
- **Do not name the precipitation id `PrecipitationEventId`.** That type
  belongs to the weather domain. Soil's is `SoilPrecipitationEventId`, and
  soil's record holds the weather one as a nullable correlation key.
- **Do not persist `SoilProfile`, `LabAnalysis`, or `NutrientPanel`.**
  They are assembled `ReadModel`s. If a task asks for a repository for one
  of them, the actual need is a new query on an `*Info` type.
- **Do not make `SoilProfileQuery` extend `EntityQuery`.** The profile has
  no stored identity of its own; the standalone shape is deliberate.
- **Do not declare the aggregate factory in soil-api.** `SoilProfileFactory`
  is a package-private concrete class in soil-core — no interface, no
  `Impl` suffix. A factory type in the api module is a review blocker.
- **Do not create a `SoilRepository` / `SoilQuery` namespace wrapper.**
  Both packages use flat top-level types today (§2).
- **Do not confuse the two saturations.** `CationBaseSaturation` is the
  % base-saturation cation block; `SaturationPct` is physical water/paste
  saturation. Different measurements, different types.
- **Do not drop a nutrient or a physical-characteristics row from a
  fixture analysis.** Panel assembly does map lookups with no null
  handling, and the read-model invariants reject nulls (§12).
- **Do not invent `SoilProfileInfoName` or `LabAnalysisInfoId`.** The
  identity types are `SoilProfileName` and `LabAnalysisId`.
