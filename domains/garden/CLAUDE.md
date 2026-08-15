# Garden Domain

**Garden is the domain of cultivation intent.** It answers: what is being grown, where,
when, and what does it require. Every type here should be justifiable as an answer to one
of those four questions.

The distinguishing feature is *intent*. A plant growing in the meadow is an observation.
The same species planted deliberately in a bed is a crop. The plant is identical; the
intent is not, and the intent is what garden models.

Design source: [`docs/garden-domain-bootstrap.md`](../../docs/garden-domain-bootstrap.md).

## Domain Vocabulary

**CropType** — Aggregate root, keyed by `CropTypeName`. The agronomic category (`tomato`,
`lettuce`, `basil`). Identity only: an optional soft `PlantName`, and nothing else.
Requirements belong to `CropProfile`, not here.

*The inclusion test:* would a lab or extension service publish a requirement table for it?
Tomato yes; Amish Paste no; *Solanum lycopersicum* not usually. FGL's reports are headed
"TOMATO SOIL ANALYSIS" with one panel covering every variety in the bed, which is exactly
the granularity `LabAnalysisInfo.cropType` points at.

**Planting** — `Entity<PlantingId>`. One variety, one place, one period — the finest grain
garden records, and the only place the agronomic and horticultural axes meet. `removedDate`
null means still growing. The spatial nullability mirrors `SoilProfileInfo`.

**GardenPlan** — `ReadModel`. A crop type assembled with its plantings. Composed on read by
`GardenPlanFactory`; never stored. Its varieties are *derived* from the plantings, not
catalogued.

**Crop** — *not modelled.* "The 2026 backyard tomato crop" is a season's growing, derivable
from plantings by type, zone and date. It would be a grouping with nothing to carry until
harvest and yield exist. Decided 2026-08-14.

**Cultivar** — *owned by plants, not garden.* `plants.cultivar.Cultivar` already carries the
Oak Vista varieties with their breeding status, fruit type and seed-saving policy. Garden
references `CultivarName` on a planting and models no cultivar of its own.

**CropProfile** — *not built.* Requirements keyed by `(source, crop, revision)`, because
different sources publish different tables and labs revise them silently. Second slice; it
needs decisions the fall 2026 soil data will inform.

## What garden is not

- **Not zone.** `zone-api` owns *where*. Garden references `ZoneName` / `SubZoneName` as
  soft names and owns no geometry, containment, or spatial hierarchy.
- **Not plants.** `plants-api` owns what *Solanum lycopersicum* is. Garden owns `tomato`, a
  cultivated category. Many-to-many, and the link is an optional soft name — a crop is not
  a taxon and this module does not use the `taxonomy` kernel.
- **Not soil.** No measurements, no lab values, no CEC. Garden supplies the requirement
  side and never stores an observed value.
- **Not the owner of soil-management events.** `AmendmentEvent`, `IrrigationEvent`,
  `TillageEvent` change soil state and stay in `soil.event`. Garden owns the crop
  lifecycle.
- **Not where assessment happens.** Applying a crop's requirements over a soil profile's
  measurements produces a status. Garden and soil are peers and neither may depend on the
  other, so that computation belongs to a module above both.

## Planting ↔ soil analysis: correlation, not a foreign key

A `Planting` carries **no `SoilProfileName`**. The link between a planting and a
crop-scoped `LabAnalysisInfo` is inferred from `zoneName` plus the date window — a bed
holds many plantings over time against one soil profile, and a second spatial key could
disagree with `zoneName`. Decided 2026-08-14; revisit only if a consumer needs the join to
be exact rather than inferred.

## Cross-domain references — typed names only

`garden-api` imports no other domain's api. Every edge is a soft reference through
`domains/identifiers`.

| Reference | Direction | Type |
|---|---|---|
| `ZoneName`, `SubZoneName` | garden → zone | `EntityName` |
| `PlantName` | garden → plants | `EntityName` |
| `CultivarName` | garden → plants | `EntityName` |
| `CropTypeName` | soil → garden (reverse) | `EntityName` |

`CropTypeName` and `PlantingId` live in `com.naturalist.garden` under `domains/identifiers`.
`CropTypeName` moved there from `com.naturalist.soil.CropName` on 2026-08-14 — soil was
simply built first, and the rename records that a soil analysis is interpreted for a crop
*type*, never for a variety or for one season's crop.

## Conventions

Inherited from soil unless stated otherwise: the `*Info` / bare-noun split, ADR-020's N=1
collapse (top-level package-private repository, top-level public query, no namespace
wrappers), `for*` on queries and `getBy*` on repositories, and a package-private concrete
aggregate factory in `garden-core` that is never declared in the api module.

Identity types are named for the concept, not the record — `CropType` is keyed by
`CropTypeName`, not `CropTypeInfoName`.

Read-only, like soil: no command surface.

## Crop type vs plant type

Plants already classifies plants on two axes — `PlantRole` (`FOOD_CROP`, `COVER_CROP`,
`NITROGEN_FIXER`, `ORNAMENTAL`, …) and `PlantLifeForm` (`ANNUAL`, `PERENNIAL`, `VINE`, …).
Those are botanical and ecological facts, true wherever the plant grows. A crop type is
agronomic: the unit requirements are published for.

Neither collapses into the other. *Brassica oleracea* is one species and five crop types —
kale, cabbage, broccoli, kohlrabi, brussels sprouts — with different spacing, different
nitrogen demand and different lab panels. "Squash" is one crop type across three *Cucurbita*
species. One `PlantName` ↔ many `CropTypeName`s, in both directions.

## Fixture data — the real 2026 beds

Eight plantings, cross-checked against the cultivars the plants catalog carries and the
zones soil samples: four tomato varieties (Amish Paste, Nick's Italian Pear, San Marzano F2,
Sungold), two basils, parsley, and an eggplant. The tomatoes came out on 2026-08-10; the
herbs and eggplant are still in.

Two properties worth keeping as fixtures exercise real cases:

- **A sub-zone holds more than one crop type.** The back yard south row carries Amish Paste
  tomatoes *and* the Black Beauty eggplant. A row is where plantings are; it claims nothing
  about what is in it.
- **`lettuce` is a crop type with no planting.** Oak Vista soil-tests for it before it goes
  in — the August 2026 FGL panel is a lettuce panel for a crop still to be planted. Which is
  why an analysis *records* the crop type it was interpreted for rather than deriving it
  from what is growing.
