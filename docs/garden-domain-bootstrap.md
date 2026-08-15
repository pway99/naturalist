# garden-domain — Bootstrap Brief

**Purpose.** Define what the garden domain *is* before any code exists, so a
session has boundaries to work against rather than a name to interpret.
Pair with `docs/briefings/framework-core.md`, `docs/briefings/shared-kernels.md`,
and `docs/briefings/soil-domain.md` (garden is soil's peer and mirrors its
structural conventions).

**Primary rule.** Nothing in this document is observed from a source tree —
`domains/garden/` does not exist yet. Everything here is a design commitment
to be implemented, not a description of what is. Where this brief is silent,
**ask rather than invent**, and follow `soil-domain.md`'s conventions by
default.

---

## 1. What the garden domain is

> **Garden is the domain of cultivation intent.** It answers: what is being
> grown, where, when, and what does it require.

That is the whole definition. Every type in the module should be justifiable
as an answer to one of those four questions.

The distinguishing feature is *intent*. A plant growing in the meadow is an
observation. The same species planted deliberately in a bed is a crop. The
plant is identical; the intent is not, and the intent is what garden models.

---

## 2. Why a naturalist application has a garden domain at all

This is the part that resists explanation, so state it directly.

The application is organised around Durrell's four levels — organism,
behavior, setting, habitat. The Oak Vista property is a *cultivated* setting.
The insects observed there are observed on planted hosts; the pollinator
meadow is sown; the tomato beds are amended, irrigated, and mulched. The
cultivation is not incidental context around the observations — it is the
principal forcing on the system being observed.

So garden is not a productivity feature bolted onto a naturalist app. It is
the *setting* level made explicit where that setting is deliberate. Without
it, every observation carries an unrecorded covariate.

The near-term driver is narrower: soil analyses are interpreted against a
crop (`LabAnalysisInfo.crop`), and there is currently nowhere for the crop
concept to live.

---

## 3. What the garden domain is not

Each of these is a boundary a session will otherwise cross.

**Garden is not zone.** `zone-api` owns *where* — the spatial subdivision of
the property. Garden references `ZoneName` / `SubZoneName` as soft typed
names and owns no geometry, no containment, no spatial hierarchy. If a task
seems to need a new spatial concept, it belongs in zone.

**Garden is not plants.** `plants-api` owns botanical and taxonomic
knowledge — what *Solanum lycopersicum* is. Garden owns `tomato`, a
cultivated category with agronomic requirements. These are different
concepts with a many-to-many relationship (one crop spans several species;
one species appears as several crops). Do **not** make `Crop` a taxon, do
**not** import the `taxonomy` kernel, and do **not** force a mandatory link
to a species. An optional soft name reference is the correct amount.

**Garden is not soil.** Garden holds no measurements, no lab values, no CEC,
no nutrient readings. `soil-api` owns those and the rule that measurements
never carry targets. Garden supplies the requirement side; it never stores an
observed value.

**Garden does not own soil-management events.** `soil.event` already models
`AmendmentEvent`, `IrrigationEvent`, `TillageEvent`, `PrecipitationEvent`,
keyed by `zoneName`. Those change soil state and stay in soil. Garden owns
the *crop* lifecycle — planting, harvest. Do not duplicate the event
sub-context, and do not add a crop foreign key to `AmendmentEvent` yet; gate
that on a consumer that actually needs it.

**Garden does not perform assessment.** Applying a crop's requirements over a
soil profile's measurements produces a status. Garden and soil are peer
modules and neither may depend on the other, so that computation lives in a
module above both — the same resolution `soil-domain.md` §10 already reached
for the mulch → `ThripsHabitatRisk` mapping. Garden supplies inputs to it and
imports nothing from it.

---

## 4. Core vocabulary

Four concepts. Resist adding a fifth in the first slice.

### `CropInfo` — the cultivated category (aggregate root)

```java
@AggregateRoot
public record CropInfo(
    CropName name,              // tomato, lettuce
    @Nullable PlantName plant   // soft reference to plants-api, optional
) implements NamedEntity<CropName>
```

Identity only. No requirements on this record — those belong to
`CropProfile`, which is provenanced and revisable while the crop is not.

### `Cultivar` — the named variety

```java
public record Cultivar(
    CultivarName name,          // amish-paste, san-marzano
    CropName cropName
) implements NamedEntity<CultivarName>
```

A cultivar belongs to exactly one crop. Real data: `amish-paste` and
`san-marzano` under `tomato`.

### `Planting` — crop in a place over a period

```java
public record Planting(
    PlantingId id,
    CropName cropName,
    @Nullable CultivarName cultivarName,
    ZoneName zoneName,
    @Nullable SubZoneName subZoneName,
    LocalDate plantedDate,
    @Nullable LocalDate removedDate,
    @Nullable String notes
) implements Entity<PlantingId>
```

This is the type that makes `LabAnalysisInfo.crop` meaningful — a March
analysis interpreted for tomato corresponds to a tomato planting in the same
zone that season. The correlation is by zone and date, deliberately soft.

`removedDate` null means currently growing. Behavior:
`isActive(LocalDate asOf)`.

The spatial nullability mirrors `SoilProfileInfo` exactly: `zoneName` always
present, `subZoneName` null when the planting covers a whole zone.

### `CropProfile` — requirements, keyed by source *(second slice)*

```java
public record CropProfile(
    CropProfileId id,
    InterpretationSourceName source,   // fgl, extension-table, ...
    CropName cropName,
    LocalDate revision,
    ...requirements...
) implements Entity<CropProfileId>
```

Keyed by `(source, crop, revision)` because different sources publish
different tables and labs revise them silently. **Do not build this in the
first slice** — it needs decisions that the fall soil data will inform.

---

## 5. Cross-domain references — typed names only

`garden-api` imports no other domain's api. Every edge is a soft reference
through `domains/identifiers`, exactly as soil does.

| Reference | Direction | Type |
|---|---|---|
| `ZoneName`, `SubZoneName` | garden → zone | `EntityName` |
| `PlantName` | garden → plants | `EntityName` |
| `CropName` | soil → garden (existing, reverse) | `EntityName` |

New identifier types to add: `CultivarName` (`EntityName`, maxLength 48),
`PlantingId` (`EntityId`), `CropProfileId` (`EntityId`, second slice).

**Relocation task.** `CropName` currently sits in `domains/identifiers` under
package `com.naturalist.soil` — an artifact of soil having been built first.
It should move to `com.naturalist.garden`. `soil-api` keeps its soft
reference and only its import line changes. Do this early; it gets harder
with every consumer.

---

## 6. Structural conventions — inherit from soil

Match `soil-domain.md` unless there is a stated reason not to:

- **`*Info` / bare-noun split.** Persisted facts are `*Info` with a
  repository, query, collection, `TestEntitySource`, and JSON catalog.
  Assembled `ReadModel`s take the bare noun and are never stored.
- **ADR-020 N=1 collapse.** One entity per package → top-level
  package-private repository, top-level public query. No namespace wrappers.
- **Identity types are named for the concept**, not the record —
  `CropInfo` is keyed by `CropName`, not `CropInfoName`.
- Repositories package-private returning raw `List`/`Optional`; query
  adapters in `garden-core` wrap them in `BehavioralCollection`s.
- Reverse lookups prefixed `for*` on queries, `getBy*` on repositories.
- Aggregate factory is a package-private concrete class in `garden-core` —
  never declared in the api module.
- Maven layout mirrors soil: `garden-api`, `garden-core`,
  `garden-repository-test`, `garden-repository-rdms` (pom only),
  `garden-test-context`, `garden-console` (read-only, `/garden`).

**Kernels.** `garden-api` should need no kernel beyond the standard set in
the first slice. It does **not** use `taxonomy` (a crop is not an organism)
and should not take `field-notes` without a stated need — a `@Nullable String
notes` on `Planting` is sufficient until proven otherwise.

---

## 7. First slice — scope

Build exactly this:

1. `CropName` relocation in `domains/identifiers`.
2. New identifiers: `CultivarName`, `PlantingId`.
3. `CropInfo` root with full port set and JSON catalog.
4. `Cultivar` with full port set and JSON catalog.
5. `Planting` with full port set, including
   `PlantingQuery.forZoneName(...)` and `forCropName(...)`.
6. `GardenPlan` assembled `ReadModel` — a crop with its cultivars and
   plantings — plus `GardenPlanFactory` in `garden-core`.
7. `GardenTestContext` and a read-only console at `/garden`.

**Out of scope for the first slice:** `CropProfile`, requirements of any
kind, harvest and yield, succession and rotation planning, seasons as a type,
germination and phenology, the `agronomy` assessment module, and any command
surface. Garden is read-only, like soil.

---

## 8. Fixture data — real, from Oak Vista

Ground the fixtures in actual plantings, as the soil module grounds its
fixtures in FGL report CH 2671853.

| Crop | Cultivars | Zone / SubZone | Planted | Removed |
|---|---|---|---|---|
| `tomato` | `amish-paste`, `san-marzano` | `box-1` (zone-scoped) | spring 2026 | Aug 2026 |
| `tomato` | `amish-paste`, `san-marzano` | `backyard` sub-zones | spring 2026 | Aug 2026 |
| `lettuce` | — | `box-1`, `backyard` | winter 2026–27 | — (active) |

These correspond to the four `SoilProfileInfo` fixtures and to the two FGL
analyses, so the correlation between a planting and a crop-scoped soil
analysis is exercisable in tests without any cross-module import.

---

## 9. Anti-patterns

- **Do not put agronomic requirements on `CropInfo`.** They belong to
  `CropProfile`, which carries a source and a revision. This mirrors soil's
  central rule (measurements never carry targets) and is the same mistake in
  the other direction.
- **Do not model `Crop` as a taxon** or import the `taxonomy` kernel. A crop
  is a cultivated category; the botanical link is an optional soft name.
- **Do not import soil-api, zone-api, or plants-api.** Typed names from
  `domains/identifiers` only.
- **Do not duplicate `soil.event`.** Amendment, irrigation, and tillage stay
  in soil.
- **Do not build assessment or status here.** Peer modules cannot depend on
  each other; the assessment engine sits above both.
- **Do not add a command surface.** Read-only, matching soil.
- **Do not invent a `Season` or `Rotation` type** in the first slice.
  Plantings have dates; derive what you need until a second consumer proves
  otherwise (FU-3 discipline).
- **Do not name the planting id `PlantId`.** That belongs to plants-api.

---

## 10. Open questions — ask, do not resolve

1. Does `Planting` reference a `SoilProfileName` directly, or is the
   correlation left to zone and date? (Leaning: zone and date. A bed can have
   several plantings and one profile.)
2. Is a cultivar ever grown without a crop parent? (Leaning: no.)
3. Should `garden-console` show plantings per zone, per crop, or both?
4. Where does the pollinator meadow sit — a planting with no harvest intent,
   or outside garden entirely?

None of these blocks the first slice. Record the answers as ADRs when they
are made.
